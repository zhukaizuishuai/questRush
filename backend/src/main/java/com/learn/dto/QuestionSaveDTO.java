package com.learn.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 题目新增 / 编辑请求（管理端）
 */
@Data
public class QuestionSaveDTO {

    /** 编辑时必传 */
    private Long id;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 1 单选 2 多选 3 判断 4 简答 */
    @NotNull(message = "题型不能为空")
    @Min(1)
    @Max(4)
    private Integer type;

    @Min(1)
    @Max(3)
    private Integer difficulty;

    @NotBlank(message = "题干不能为空")
    private String title;

    @Valid
    private List<OptionItem> options;

    /** 客观题必填：单选/判断 A，多选 ABD */
    private String answer;

    /** 简答题参考答案 */
    private String answerText;

    private String analysis;

    private Integer isVip;

    private Integer status;

    @Data
    public static class OptionItem {
        @NotBlank(message = "选项标识不能为空")
        private String optionCode;
        @NotBlank(message = "选项内容不能为空")
        private String optionContent;
    }
}
