package com.learn.controller;

import jakarta.annotation.Resource;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.dto.PracticeSessionDTO;
import com.learn.dto.SubmitDTO;
import com.learn.service.PracticeService;
import com.learn.vo.QuestionPracticeVO;
import com.learn.vo.QuestionSubmitVO;
import com.learn.vo.QuestionWrongVO;
import com.learn.vo.StatsVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 刷题接口（文档 6.2）：会话、逐题作答、错题本、复习队列、统计
 */
@RestController
@RequestMapping("/api/practice")
public class PracticeController {

    @Resource
    private PracticeService practiceService;

    /** 创建刷题会话：返回 sessionId 与题目总数（文档 4.3） */
    @PostMapping("/session")
    public Result<Map<String, Object>> createSession(@Valid @RequestBody PracticeSessionDTO dto) {
        return Result.ok(practiceService.createSession(dto, StpUtil.getLoginIdAsLong()));
    }

    /** 按 sessionId + index 取题（答题前 VO，不含答案） */
    @GetMapping("/next")
    public Result<QuestionPracticeVO> next(@RequestParam String sessionId,
                                           @RequestParam int index) {
        return Result.ok(practiceService.next(sessionId, index, StpUtil.getLoginIdAsLong()));
    }

    /** 提交答题（返回 QuestionSubmitVO，此阶段才下发答案与解析） */
    @PostMapping("/submit")
    public Result<QuestionSubmitVO> submit(@Valid @RequestBody SubmitDTO dto) {
        return Result.ok(practiceService.submit(dto, StpUtil.getLoginIdAsLong()));
    }

    /** 错题本（越权条目脱敏，文档 4.1） */
    @GetMapping("/wrong/list")
    public Result<PageResult<QuestionWrongVO>> wrongList(@RequestParam(defaultValue = "1") long pageNum,
                                                         @RequestParam(defaultValue = "10") long pageSize) {
        IPage<QuestionWrongVO> page = practiceService.wrongBook(pageNum, pageSize, StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /** 复习队列（mastered=0 且 next_review_time <= NOW()，文档 4.3） */
    @GetMapping("/review/list")
    public Result<PageResult<QuestionWrongVO>> reviewList(@RequestParam(defaultValue = "1") long pageNum,
                                                          @RequestParam(defaultValue = "10") long pageSize) {
        IPage<QuestionWrongVO> page = practiceService.reviewList(pageNum, pageSize, StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /** 统计：总答题数、正确率、按分类正确率、连续打卡天数 */
    @GetMapping("/stats")
    public Result<StatsVO> stats() {
        return Result.ok(practiceService.stats(StpUtil.getLoginIdAsLong()));
    }
}
