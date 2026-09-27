package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.CategorySaveDTO;
import com.learn.entity.Question;
import com.learn.entity.QuestionCategory;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.QuestionCategoryMapper;
import com.learn.mapper.QuestionMapper;
import com.learn.service.AdminCategoryService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 管理端分类服务实现（文档 3.2）。
 * 删除三重校验：
 * 1. 存在未删除的子分类 → 拒绝
 * 2. 存在启用状态的题目（status=1）→ 拒绝
 * 3. 仅有已下架或已删除题目 → 允许
 */
@Service
public class AdminCategoryServiceImpl implements AdminCategoryService {

    @Resource
    private QuestionCategoryMapper categoryMapper;

    @Resource
    private QuestionMapper questionMapper;

    @Override
    public void save(CategorySaveDTO dto) {
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (parentId != 0) {
            QuestionCategory parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "父分类不存在");
            }
            // 只允许两级：父分类必须是一级分类（文档 3.2 题目只挂叶子）
            if (parent.getParentId() != null && parent.getParentId() != 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "最多支持两级分类，不能在子分类下再建子分类");
            }
            assertParentHasNoQuestions(parentId);
        }
        QuestionCategory category = new QuestionCategory();
        category.setName(dto.getName());
        category.setParentId(parentId);
        category.setSort(dto.getSort() == null ? 0 : dto.getSort());
        categoryMapper.insert(category);
    }

    @Override
    public void update(CategorySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "分类 id 不能为空");
        }
        QuestionCategory exists = categoryMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        Long parentId = dto.getParentId() == null ? exists.getParentId() : dto.getParentId();
        if (parentId == null) {
            parentId = 0L;
        }
        if (parentId.equals(dto.getId())) {
            throw new BizException(ResultCode.PARAM_ERROR, "父分类不能是自己");
        }
        if (parentId != 0) {
            QuestionCategory parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "父分类不存在");
            }
            if (parent.getParentId() != null && parent.getParentId() != 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "最多支持两级分类，不能在子分类下再建子分类");
            }
            // 目标父分类若已挂题目，不能再被当作父级（否则题目会变成非叶子）
            assertParentHasNoQuestions(parentId);
            // 自身若已有子分类，也不能再降级为别人的子分类（否则出现三级）
            Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>()
                    .eq(QuestionCategory::getParentId, dto.getId()));
            if (childCount > 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "该分类下已有子分类，不能再作为子分类");
            }
        }
        QuestionCategory update = new QuestionCategory();
        update.setId(dto.getId());
        update.setName(dto.getName());
        update.setParentId(parentId);
        if (dto.getSort() != null) {
            update.setSort(dto.getSort());
        }
        categoryMapper.updateById(update);
    }

    /** 作为父分类前必须自身没有题目，否则其下题目会变成「非叶子分类下的题目」 */
    private void assertParentHasNoQuestions(Long parentId) {
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, parentId));
        if (count > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该分类下已有题目，不能再建子分类");
        }
    }

    @Override
    public void delete(Long id) {
        if (categoryMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "分类不存在");
        }
        // 校验 1：存在未删除的子分类
        Long childCount = categoryMapper.selectCount(new LambdaQueryWrapper<QuestionCategory>()
                .eq(QuestionCategory::getParentId, id));
        if (childCount > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "请先删除该分类下的子分类");
        }
        // 校验 2：存在启用状态的题目（MP 逻辑删除自动过滤已删题目）
        Long activeCount = questionMapper.selectCount(new LambdaQueryWrapper<Question>()
                .eq(Question::getCategoryId, id)
                .eq(Question::getStatus, 1));
        if (activeCount > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "该分类下存在启用状态的题目，请先下架或迁移题目");
        }
        // 校验 3 通过：允许删除（逻辑删除）
        categoryMapper.deleteById(id);
    }
}
