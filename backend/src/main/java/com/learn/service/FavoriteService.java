package com.learn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.vo.FavoriteItemVO;

import java.util.Map;

/**
 * 收藏服务（文档 3.7 / 4.1）：toggle 语义，越权条目脱敏
 */
public interface FavoriteService {

    /** 收藏 / 取消收藏，返回最终状态 */
    Map<String, Object> toggle(Long questionId, Long userId);

    /** 收藏列表（越权条目脱敏，文档 4.1） */
    IPage<FavoriteItemVO> list(long pageNum, long pageSize, Long userId);
}
