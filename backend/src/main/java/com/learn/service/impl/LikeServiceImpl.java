package com.learn.service.impl;

import com.learn.checker.QuestionAccessChecker;
import com.learn.entity.Question;
import com.learn.mapper.QuestionLikeMapper;
import com.learn.mapper.QuestionMapper;
import com.learn.service.LikeService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 点赞服务实现：文档 4.6 五步事务原样落地。
 *
 * 事务内顺序（三个坑缺一即失效）：
 * 1. 占位 upsert —— 确保行存在并持排他行锁；省掉此步，并发 SELECT FOR UPDATE 的
 *    gap lock + 插入意向锁会死锁
 * 2. SELECT ... FOR UPDATE 当前读 —— RR 下普通 SELECT 是快照读，读不到并发事务的修改
 * 3. 推导 delta —— MySQL 的 ON DUPLICATE KEY UPDATE 不返回行值（无 RETURNING）
 * 4. UPDATE 最终状态
 * 5. like_count 原子增减（changeLikeCount 内含 >= 0 保护，不允许负数）
 *
 * 隔离级别用 MySQL 默认 REPEATABLE READ，无需调整（文档 4.6）。
 */
@Service
public class LikeServiceImpl implements LikeService {

    @Resource
    private QuestionLikeMapper likeMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private QuestionAccessChecker checker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggle(Long questionId, Long userId) {
        // 点赞前先走 VIP 鉴权（文档 4.6：VIP 题无权限时 40301）
        checker.checkReadable(questionId, userId);

        // 1. 占位 upsert：行不存在 → 插入 deleted=1；行已存在 → 空更新，同样加排他锁
        likeMapper.upsertPlaceholder(userId, questionId);

        // 2. 当前读：行此时必然存在，FOR UPDATE 取最新 deleted
        Integer oldDeleted = likeMapper.selectDeletedForUpdate(userId, questionId);

        // 3. 推导目标状态与计数增量：0=已点赞 1=已取消
        int newDeleted = (oldDeleted != null && oldDeleted == 0) ? 1 : 0;
        int delta = (newDeleted == 0) ? 1 : -1;

        // 4. 写入最终状态
        likeMapper.updateDeleted(userId, questionId, newDeleted);

        // 5. 更新冗余计数，不允许减成负数（SQL 带 like_count + delta >= 0 保护）
        questionMapper.changeLikeCount(questionId, delta);

        Question question = questionMapper.selectById(questionId);
        return Map.of(
                "liked", newDeleted == 0,
                "likeCount", question == null || question.getLikeCount() == null ? 0 : question.getLikeCount()
        );
    }
}
