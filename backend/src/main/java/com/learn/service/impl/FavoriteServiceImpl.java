package com.learn.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.checker.QuestionAccessChecker;
import com.learn.entity.UserFavorite;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserFavoriteMapper;
import com.learn.service.FavoriteService;
import com.learn.util.PageUtil;
import com.learn.vo.FavoriteItemVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 收藏服务实现（文档 3.0 / 3.7 / 4.1）。
 * toggle 语义直接翻转同一行 deleted，永不新增行；
 * 唯一键不含 deleted，所以必须用原生 SQL 查含软删在内的行。
 */
@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Resource
    private UserFavoriteMapper favoriteMapper;

    @Resource
    private QuestionAccessChecker checker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> toggle(Long questionId, Long userId) {
        UserFavorite row = favoriteMapper.selectAnyRow(userId, questionId);
        boolean currentlyFavorited = row != null && row.getDeleted() != null && row.getDeleted() == 0;
        // 只有「新增收藏」方向需要鉴权（防收藏 VIP 题，文档 4.1）；
        // 取消收藏不做鉴权，否则题目一旦下架/软删，用户就再也清不掉这条「幽灵收藏」。
        if (!currentlyFavorited) {
            checker.checkReadable(questionId, userId);
        }
        // 单语句原子翻转：行不存在 → 插入 deleted=0；已存在 → 就地翻转。
        // 并发首次收藏走 ON DUPLICATE KEY UPDATE，不会撞唯一键（文档 3.0 toggle 语义）。
        favoriteMapper.upsertToggle(userId, questionId);
        UserFavorite after = favoriteMapper.selectAnyRow(userId, questionId);
        boolean favorited = after != null && after.getDeleted() != null && after.getDeleted() == 0;
        return Map.of("favorited", favorited);
    }

    @Override
    public IPage<FavoriteItemVO> list(long pageNum, long pageSize, Long userId) {
        Page<FavoriteItemVO> page = new Page<>(PageUtil.num(pageNum), PageUtil.size(pageSize));
        IPage<FavoriteItemVO> result = favoriteMapper.selectFavoritePage(page, userId);
        // 越权条目脱敏（文档 4.1）：只保留 id + 分类名，题干置空。
        // isVip 由 SQL 一并查出，用一次 canReadVip 判定即可，避免逐条 checkReadable 的 N+1。
        List<FavoriteItemVO> records = result.getRecords();
        if (records != null && !checker.canReadVip(userId)) {
            for (FavoriteItemVO vo : records) {
                if (vo.getTitle() != null && Integer.valueOf(1).equals(vo.getIsVip())) {
                    vo.setTitle(null);
                }
            }
        }
        return result;
    }
}
