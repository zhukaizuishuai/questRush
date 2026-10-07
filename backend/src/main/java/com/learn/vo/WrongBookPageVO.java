package com.learn.vo;

import lombok.Data;

import java.util.List;

/**
 * 错题本 / 复习队列分页返回（文档 4.3）。
 *
 * 在 PageResult 之外额外回传 answerableCount —— 整个队列中「当前可作答」的条目数，
 * 与选题 SQL（selectReviewIds / selectWrongIds）的过滤条件完全一致。
 *
 * 为什么必须由服务端给：列表接口用 LEFT JOIN，展示层能看到全部条目（越权条目脱敏置 title=null）；
 * 而 total 是全量条数。前端若靠「当前页 title != null 的条数」估算，会把分页误当成全量，
 * 导致第一页恰好全是脱敏题时误禁用「开始复习」按钮 —— 而会话其实能从后续页取到可做的题。
 */
@Data
public class WrongBookPageVO {

    /** 当前页条目（越权条目 title 为 null） */
    private List<QuestionWrongVO> list;

    /** 全量条数（含不可作答条目） */
    private long total;

    private long pageNum;

    private long pageSize;

    /** 全队列可作答条目数（已过滤下架 / 删除 / 无权限），与选题 SQL 同口径 */
    private long answerableCount;
}
