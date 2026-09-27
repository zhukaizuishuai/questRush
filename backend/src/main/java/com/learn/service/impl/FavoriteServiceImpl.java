package com.learn.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.checker.QuestionAccessChecker;
import com.learn.entity.UserFavorite;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserFavoriteMapper;
import com.learn.service.FavoriteService;
import com.learn.vo.FavoriteItemVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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
    public Map<String, Object> toggle(Long questionId, Long userId) {
        // 防收藏 VIP 题（文档 4.1 鉴权矩阵）
        checker.checkReadable(questionId, userId);

        UserFavorite row = favoriteMapper.selectAnyRow(userId, questionId);
        boolean favorited;
        if (row == null) {
            // 首次收藏：插入 deleted=0
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setQuestionId(questionId);
            favorite.setDeleted(0);
            favoriteMapper.insert(favorite);
            favorited = true;
        } else {
            // 翻转同一行
            int newDeleted = row.getDeleted() != null && row.getDeleted() == 0 ? 1 : 0;
            favoriteMapper.updateDeletedById(row.getId(), newDeleted);
            favorited = newDeleted == 0;
        }
        return Map.of("favorited", favorited);
    }

    @Override
    public IPage<FavoriteItemVO> list(long pageNum, long pageSize, Long userId) {
        Page<FavoriteItemVO> page = new Page<>(pageNum, Math.min(pageSize, 100));
        IPage<FavoriteItemVO> result = favoriteMapper.selectFavoritePage(page, userId);
        // 越权条目脱敏：只保留 id + 分类名，题干置空（文档 4.1）
        List<FavoriteItemVO> records = result.getRecords();
        if (records != null) {
            for (FavoriteItemVO vo : records) {
                try {
                    checker.checkReadable(vo.getQuestionId(), userId);
                } catch (BizException e) {
                    vo.setTitle(null);
                }
            }
        }
        return result;
    }
}
