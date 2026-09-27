package com.learn.service;

import com.learn.dto.CategorySaveDTO;

/**
 * 管理端分类服务（文档 3.2 / 6.4）：CRUD + 删除三重校验
 */
public interface AdminCategoryService {

    void save(CategorySaveDTO dto);

    void update(CategorySaveDTO dto);

    /** 删除三重校验：子分类存在拒绝 / 启用题目存在拒绝 / 其余允许 */
    void delete(Long id);
}
