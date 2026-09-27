package com.learn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 笔记保存请求（文档 3.8：upsert）
 */
@Data
public class NoteSaveDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long questionId;

    @NotBlank(message = "笔记内容不能为空")
    @Size(max = 65535, message = "笔记内容过长")
    private String content;
}
