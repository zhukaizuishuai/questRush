package com.learn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类新增 / 编辑（文档 3.2：题目只允许挂叶子分类）
 */
@Data
public class CategorySaveDTO {

    /** 编辑时必传 */
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100, message = "分类名称过长")
    private String name;

    /** 0 = 一级分类 */
    private Long parentId;

    private Integer sort;
}
