package com.learn.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.dto.NoteSaveDTO;
import com.learn.service.NoteService;
import com.learn.vo.NoteVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 笔记接口（文档 6.2）：保存走 upsert，仅本人可见
 */
@RestController
@RequestMapping("/api/note")
public class NoteController {

    @Resource
    private NoteService noteService;

    /** 保存笔记（防给 VIP 题写笔记，文档 4.1） */
    @PostMapping("/save")
    public Result<Void> save(@Valid @RequestBody NoteSaveDTO dto) {
        noteService.save(dto.getQuestionId(), dto.getContent(), StpUtil.getLoginIdAsLong());
        return Result.ok();
    }

    /** 获取某题笔记（本人） */
    @GetMapping("/detail")
    public Result<NoteVO> detail(@RequestParam Long questionId) {
        return Result.ok(noteService.detail(questionId, StpUtil.getLoginIdAsLong()));
    }

    /** 笔记列表（本人，分页；越权条目题干脱敏，文档 4.1） */
    @GetMapping("/list")
    public Result<PageResult<NoteVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        IPage<NoteVO> page = noteService.list(pageNum, pageSize, StpUtil.getLoginIdAsLong());
        return Result.ok(PageResult.of(page, page.getRecords()));
    }
}
