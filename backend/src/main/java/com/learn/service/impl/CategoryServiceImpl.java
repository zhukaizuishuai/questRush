package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.entity.QuestionCategory;
import com.learn.mapper.QuestionCategoryMapper;
import com.learn.service.CategoryService;
import com.learn.vo.CategoryVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类树构建：一级分类 parent_id=0，二级挂在一级下
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    @Resource
    private QuestionCategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> tree() {
        List<QuestionCategory> all = categoryMapper.selectList(new LambdaQueryWrapper<QuestionCategory>()
                .orderByDesc(QuestionCategory::getSort)
                .orderByAsc(QuestionCategory::getId));
        Map<Long, List<CategoryVO>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != null && c.getParentId() != 0)
                .collect(Collectors.groupingBy(QuestionCategory::getParentId,
                        Collectors.mapping(this::toVO, Collectors.toList())));
        List<CategoryVO> roots = new ArrayList<>();
        for (QuestionCategory c : all) {
            if (c.getParentId() == null || c.getParentId() == 0) {
                CategoryVO vo = toVO(c);
                vo.setChildren(childrenMap.getOrDefault(c.getId(), new ArrayList<>()));
                roots.add(vo);
            }
        }
        return roots;
    }

    private CategoryVO toVO(QuestionCategory c) {
        CategoryVO vo = new CategoryVO();
        vo.setId(c.getId());
        vo.setName(c.getName());
        vo.setParentId(c.getParentId());
        vo.setSort(c.getSort());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
