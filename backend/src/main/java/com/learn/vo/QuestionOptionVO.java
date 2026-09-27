package com.learn.vo;

import lombok.Data;

/**
 * 选项 VO：仅 code 与 content（文档 4.2）
 */
@Data
public class QuestionOptionVO {

    private String optionCode;
    private String optionContent;

    public QuestionOptionVO() {
    }

    public QuestionOptionVO(String optionCode, String optionContent) {
        this.optionCode = optionCode;
        this.optionContent = optionContent;
    }
}
