package com.learn.controller;

import com.learn.common.Result;
import com.learn.service.CategoryService;
import com.learn.vo.CategoryVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口（文档 6.2：分类树）
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.tree());
    }
}
