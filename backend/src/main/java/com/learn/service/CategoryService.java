package com.learn.service;

import com.learn.vo.CategoryVO;

import java.util.List;

/**
 * 分类服务（文档 6.2：分类树）
 */
public interface CategoryService {

    List<CategoryVO> tree();
}
