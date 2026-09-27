package com.learn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 题目上下架请求（管理端）。改为 JSON body 传参，与前端 PUT body {id, status} 对齐。
 */
@Data
public class QuestionStatusDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long id;

    @NotNull(message = "状态不能为空")
    @Min(0)
    @Max(1)
    private Integer status;
}
