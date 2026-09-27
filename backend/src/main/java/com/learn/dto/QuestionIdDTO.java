package com.learn.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用 questionId 请求体：收藏 toggle / 点赞 toggle / 笔记查询等
 */
@Data
public class QuestionIdDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long questionId;
}
