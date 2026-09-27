package com.learn.checker;

import com.learn.entity.Question;
import com.learn.entity.User;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionMapper;
import com.learn.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * VIP 资源统一鉴权（文档 4.1）：
 * 1. 题目不存在或已下架 → 404
 * 2. role = admin → 放行（后台运维预览）
 * 3. is_vip = 1 且不在 VIP 有效期内 → 40301（VIP_REQUIRED）
 *
 * 覆盖文档 4.1 鉴权矩阵的逐条入口：详情、提交、收藏 toggle、收藏列表、笔记保存、笔记查询、
 * 错题本、复习队列、点赞；列表与搜索在 SQL 层按 is_vip 过滤（QuestionMapper.vipFilter）。
 */
@Component
public class QuestionAccessChecker {

    @Resource
    private QuestionMapper questionMapper;

    @Resource
    private UserMapper userMapper;

    /**
     * 按题目 id 鉴权（逐条防 id 遍历）
     */
    public Question checkReadable(Long questionId, Long userId) {
        Question question = questionMapper.selectById(questionId);
        if (question == null || question.getStatus() == 0) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return checkReadable(question, userId);
    }

    /**
     * 按已查出的题目鉴权（避免重复查库）
     */
    public Question checkReadable(Question question, Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && "admin".equals(user.getRole())) {
            return question;
        }
        if (question.getIsVip() == 1 && !isVipValid(user)) {
            throw new BizException(ResultCode.VIP_REQUIRED);
        }
        return question;
    }

    public boolean isVipValid(Long userId) {
        return isVipValid(userMapper.selectById(userId));
    }

    /**
     * VIP 有效性实时比较 vip_expire_time 与 NOW()（文档 10.1：不在到期时跑定时任务）
     */
    public boolean isVipValid(User user) {
        return user != null
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now());
    }

    /**
     * 列表 / 搜索 SQL 层过滤用：admin 或 VIP 有效即可见 is_vip=1 题目
     */
    public boolean canReadVip(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null && ("admin".equals(user.getRole()) || isVipValid(user));
    }
}
