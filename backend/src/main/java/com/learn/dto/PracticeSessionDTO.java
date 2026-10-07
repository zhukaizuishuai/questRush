package com.learn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 刷题会话创建请求（文档 4.3）：分类、模式、数量、难度、选题来源
 */
@Data
public class PracticeSessionDTO {

    /**
     * 选题来源（不传等价 all）：
     * all    常规刷题，按 categoryId / difficulty 从题库选题（文档 4.3）
     * review 复习模式，从遗忘曲线复习队列选题（mastered=0 且 next_review_time <= NOW()），
     *         选题时按 next_review_time 升序，越早到期越先复习；此时忽略 categoryId / difficulty
     * wrong  错题重做模式，从错题本选题（mastered=0 且 is_correct=0），按最近作答时间倒序
     */
    @jakarta.validation.constraints.Pattern(regexp = "all|review|wrong", message = "选题来源须为 all / review / wrong")
    private String source = "all";

    /** 分类 id，可为空表示全部题目（source=review / wrong 时忽略） */
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
