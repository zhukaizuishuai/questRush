package com.learn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 提交答题请求（文档 4.3）。answer 为作答内容：单选/判断 "A"，多选 "ABD"（乱序亦可），简答为文本。
 */
@Data
public class SubmitDTO {

    @NotNull(message = "题目 id 不能为空")
    private Long questionId;

    @NotBlank(message = "答案不能为空")
    @Size(max = 500, message = "答案过长")
    private String answer;
}
