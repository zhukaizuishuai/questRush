package com.learn.controller.admin;

import jakarta.annotation.Resource;
import com.learn.common.Result;
import com.learn.dto.CategorySaveDTO;
import com.learn.service.AdminCategoryService;
import com.learn.service.CategoryService;
import com.learn.vo.CategoryVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端分类管理（文档 6.4）。删除走三重校验（文档 3.2）。
 */
@RestController
@RequestMapping("/api/admin/category")
public class AdminCategoryController {

    @Resource
    private AdminCategoryService adminCategoryService;

    @Resource
    private CategoryService categoryService;

    /** 分类树（管理端复用前台树结构） */
    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.tree());
    }

    /** 新增分类 */
    @PostMapping
    public Result<Void> save(@Valid @RequestBody CategorySaveDTO dto) {
        adminCategoryService.save(dto);
        return Result.ok();
    }

    /** 编辑分类 */
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CategorySaveDTO dto) {
        adminCategoryService.update(dto);
        return Result.ok();
    }

    /** 删除分类（三重校验：子分类 / 启用题目，文档 3.2） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminCategoryService.delete(id);
        return Result.ok();
    }
}
