package com.learn.controller;

import jakarta.annotation.Resource;
import cn.dev33.satoken.stp.StpUtil;
import com.learn.common.Result;
import com.learn.dto.QuestionIdDTO;
import com.learn.service.LikeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 点赞接口（文档 4.6 / 6.2）：toggle，返回最新 likeCount 与 liked
 */
@RestController
@RequestMapping("/api/like")
public class LikeController {

    @Resource
    private LikeService likeService;

    /** 点赞 / 取消点赞（五步事务，防对 VIP 题点赞） */
    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggle(@Valid @RequestBody QuestionIdDTO dto) {
        return Result.ok(likeService.toggle(dto.getQuestionId(), StpUtil.getLoginIdAsLong()));
    }
}
