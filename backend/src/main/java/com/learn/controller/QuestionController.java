package com.learn.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.service.QuestionService;
import com.learn.vo.QuestionListVO;
import com.learn.vo.QuestionPracticeVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 题目接口（文档 6.2）：列表 / 搜索 SQL 层按权限过滤 is_vip；详情逐条鉴权。
 */
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Resource
    private QuestionService questionService;

    /** 题目列表（QuestionListVO，按权限过滤，文档 4.1） */
    @GetMapping("/list")
    public Result<PageResult<QuestionListVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize,
                                                   @RequestParam(required = false) Long categoryId,
                                                   @RequestParam(required = false) Integer difficulty) {
        IPage<QuestionListVO> page = questionService.pageList(pageNum, pageSize, categoryId, null, difficulty,
                StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /** 题目搜索（复用列表的过滤条件，文档 4.1） */
    @GetMapping("/search")
    public Result<PageResult<QuestionListVO>> search(@RequestParam(defaultValue = "1") long pageNum,
                                                     @RequestParam(defaultValue = "10") long pageSize,
                                                     @RequestParam(required = false) Long categoryId,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) Integer difficulty) {
        IPage<QuestionListVO> page = questionService.pageList(pageNum, pageSize, categoryId, keyword, difficulty,
                StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /** 答题前详情（QuestionPracticeVO，不含答案，文档 4.2） */
    @GetMapping("/detail")
    public Result<QuestionPracticeVO> detail(@RequestParam Long questionId) {
        return Result.ok(questionService.detail(questionId, StpUtil.getLoginIdAsLong()));
    }
}
