package com.learn.controller;

import jakarta.annotation.Resource;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.dto.QuestionIdDTO;
import com.learn.service.FavoriteService;
import com.learn.vo.FavoriteItemVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 收藏接口（文档 6.2）：toggle 语义，越权条目脱敏
 */
@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {

    @Resource
    private FavoriteService favoriteService;

    /** 收藏 / 取消收藏（防收藏 VIP 题，文档 4.1） */
    @PostMapping("/toggle")
    public Result<Map<String, Object>> toggle(@Valid @RequestBody QuestionIdDTO dto) {
        return Result.ok(favoriteService.toggle(dto.getQuestionId(), StpUtil.getLoginIdAsLong()));
    }

    /** 收藏列表（越权条目脱敏，文档 4.1） */
    @GetMapping("/list")
    public Result<PageResult<FavoriteItemVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        IPage<FavoriteItemVO> page = favoriteService.list(pageNum, pageSize, StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }
}
