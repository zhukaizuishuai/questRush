package com.learn.service;

import java.util.Map;

/**
 * 点赞服务（文档 4.6）：五步事务 toggle
 */
public interface LikeService {

    /**
     * 点赞 / 取消点赞，返回 {liked, likeCount}
     */
    Map<String, Object> toggle(Long questionId, Long userId);
}
