package com.learn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 刷题会话创建请求（文档 4.3）：分类、模式、数量、难度
 */
@Data
public class PracticeSessionDTO {

    /** 分类 id，可为空表示全部题目 */
    private Long categoryId;

    /** order 顺序 / random 随机 */
    @NotNull(message = "刷题模式不能为空")
    @jakarta.validation.constraints.Pattern(regexp = "order|random", message = "刷题模式须为 order 或 random")
    private String mode;

    @Min(value = 1, message = "题目数量至少 1")
    @Max(value = 100, message = "题目数量最多 100")
    private Integer count;

    /** 1 简单 2 中等 3 困难，可选 */
    @Min(1)
    @Max(3)
    private Integer difficulty;
}
