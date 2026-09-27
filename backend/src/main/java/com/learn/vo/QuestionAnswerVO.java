package com.learn.vo;

import lombok.Data;

/**
 * 题目答案 / 解析 VO（GET /api/question/answer 主动查看场景）。
 *
 * 权衡说明（产品决策）：答案分级原则是「答题前不下发答案」（文档 4.2），
 * 但产品要求详情页支持主动查看答案与解析，故提供独立端点。
 * 注意：任何主动下发答案的接口都能被 F12 抓包获取，此为可接受的取舍，
 * 刷题/会话/提交链路仍严格不返回答案字段。
 */
@Data
public class QuestionAnswerVO {

    /** 客观题标准答案（单选 A / 多选归一化 ABD / 判断 A|B），简答题为空 */
    private String answer;

    /** 简答题参考答案（Markdown） */
    private String answerText;

    /** 题目解析（Markdown） */
    private String analysis;
}
