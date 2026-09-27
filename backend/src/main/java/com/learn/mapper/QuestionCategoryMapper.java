package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.QuestionCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 题目分类表 Mapper（文档 3.2）
 */
@Mapper
public interface QuestionCategoryMapper extends BaseMapper<QuestionCategory> {
}
