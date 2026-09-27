package com.learn.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * Excel 导入模板行（文档 4.4 列定义），导出复用同一结构。
 * 一行一题：分类名称 | 题型 | 难度 | 题干 | 选项 | 正确答案 | 解析 | 是否VIP
 */
@Data
@ColumnWidth(24)
public class QuestionExcelDTO {

    @ExcelProperty("分类名称")
    private String categoryName;

    @ExcelProperty("题型")
    private String typeName;

    @ExcelProperty("难度")
    private String difficultyName;

    @ExcelProperty("题干")
    @ColumnWidth(50)
    private String title;

    @ExcelProperty("选项")
    @ColumnWidth(50)
    private String options;

    @ExcelProperty("正确答案")
    private String answer;

    @ExcelProperty("解析")
    @ColumnWidth(50)
    private String analysis;

    @ExcelProperty("是否VIP")
    private String vipFlag;

    /** 选项解析结果（导入流程内部使用，不参与 Excel 映射） */
    public record OptionParsed(String code, String content, int sort) {
    }
}
