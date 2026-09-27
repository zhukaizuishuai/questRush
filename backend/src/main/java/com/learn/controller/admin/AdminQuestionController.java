package com.learn.controller.admin;

import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.dto.QuestionSaveDTO;
import com.learn.dto.QuestionStatusDTO;
import com.learn.service.AdminQuestionService;
import com.learn.service.QuestionExcelService;
import com.learn.vo.ImportResultVO;
import com.learn.vo.QuestionAdminVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 管理端题库管理（文档 6.4）：单条 CRUD、上下架、Excel 模板/导入/导出、重算点赞、数据统计。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminQuestionController {

    @Resource
    private AdminQuestionService adminQuestionService;

    @Resource
    private QuestionExcelService questionExcelService;

    // ---------- 题目 CRUD ----------

    /** 题目分页（QuestionAdminVO 全字段，仅 admin）；支持 type/isVip/difficulty/status/keyword 筛选 */
    @GetMapping("/question/list")
    public Result<PageResult<QuestionAdminVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) Long categoryId,
                                                    @RequestParam(required = false) String keyword,
                                                    @RequestParam(required = false) Integer type,
                                                    @RequestParam(required = false) Integer isVip,
                                                    @RequestParam(required = false) Integer difficulty,
                                                    @RequestParam(required = false) Integer status) {
        IPage<QuestionAdminVO> page = adminQuestionService.page(pageNum, pageSize, categoryId, keyword,
                type, isVip, difficulty, status);
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /** 题目详情（含答案与选项） */
    @GetMapping("/question/detail")
    public Result<QuestionAdminVO> detail(@RequestParam Long id) {
        return Result.ok(adminQuestionService.detail(id));
    }

    /** 新增题目 */
    @PostMapping("/question")
    public Result<Void> save(@Valid @RequestBody QuestionSaveDTO dto) {
        adminQuestionService.save(dto);
        return Result.ok();
    }

    /** 编辑题目（选项整体覆盖，文档 4.4 OVERWRITE 语义） */
    @PutMapping("/question")
    public Result<Void> update(@Valid @RequestBody QuestionSaveDTO dto) {
        adminQuestionService.update(dto);
        return Result.ok();
    }

    /** 上下架（JSON body {id, status}） */
    @PutMapping("/question/status")
    public Result<Void> updateStatus(@Valid @RequestBody QuestionStatusDTO dto) {
        adminQuestionService.updateStatus(dto.getId(), dto.getStatus());
        return Result.ok();
    }

    /** 删除题目（逻辑删除） */
    @DeleteMapping("/question/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminQuestionService.delete(id);
        return Result.ok();
    }

    /** 按点赞表重算 like_count，数据兜底（文档 4.6） */
    @PostMapping("/question/recalc-like")
    public Result<Map<String, Object>> recalcLike() {
        int affected = adminQuestionService.recalcLike();
        return Result.ok(Map.of("affected", affected));
    }

    // ---------- Excel 导入导出（文档 4.4） ----------

    /** 下载导入模板 */
    @GetMapping("/excel/template")
    public void template(HttpServletResponse response) {
        questionExcelService.downloadTemplate(response);
    }

    /** 批量导入：duplicateStrategy = SKIP/OVERWRITE/ERROR，返回成功/跳过/失败明细 */
    @PostMapping("/excel/import")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "SKIP") String duplicateStrategy) {
        return Result.ok(questionExcelService.importExcel(file, duplicateStrategy));
    }

    /** 按条件批量导出（分类 / 是否VIP / 难度 / 状态） */
    @GetMapping("/excel/export")
    public void export(@RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Integer isVip,
                       @RequestParam(required = false) Integer difficulty,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) {
        questionExcelService.export(categoryId, isVip, difficulty, status, response);
    }

    // ---------- 数据统计 ----------

    /** 数据统计：用户数、题目数、VIP 用户数、日均答题量（文档 6.4） */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminQuestionService.stats());
    }
}
