package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.entity.UserQuestionRecord;
import com.learn.vo.QuestionWrongVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户做题状态表 Mapper（文档 3.5 / 4.3）。
 * 写入统一 INSERT ... ON DUPLICATE KEY UPDATE，计数用 total_count = total_count + 1 原子累加（文档 4.3 并发提交）。
 * 遗忘曲线：答对 continuous_correct+1、review_level+1，next_review_time 按间隔 1/2/4/7/15 天顺延；
 * 答错归零、next_review_time = NOW()+1天；continuous_correct>=3 或 review_level>=5 → mastered=1 且 next_review_time=NULL。
 */
@Mapper
public interface UserQuestionRecordMapper extends BaseMapper<UserQuestionRecord> {

    /**
     * 答对分支：review_level 与 next_review_time 在 SQL 内基于旧值推导，保证并发原子性。
     */
    @Insert("INSERT INTO user_question_record " +
            "(user_id, question_id, last_answer, is_correct, total_count, wrong_count, continuous_correct, review_level, next_review_time, mastered, submit_time) " +
            "VALUES (#{userId}, #{questionId}, #{answer}, 1, 1, 0, 1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY), 0, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "last_answer = VALUES(last_answer), is_correct = 1, total_count = total_count + 1, " +
            "continuous_correct = continuous_correct + 1, " +
            "review_level = LEAST(review_level + 1, 5), " +
            "mastered = IF(continuous_correct + 1 >= 3 OR LEAST(review_level + 1, 5) >= 5, 1, 0), " +
            "next_review_time = IF(continuous_correct + 1 >= 3 OR LEAST(review_level + 1, 5) >= 5, NULL, " +
            "  CASE LEAST(review_level + 1, 5) " +
            "    WHEN 1 THEN DATE_ADD(NOW(), INTERVAL 1 DAY) " +
            "    WHEN 2 THEN DATE_ADD(NOW(), INTERVAL 2 DAY) " +
            "    WHEN 3 THEN DATE_ADD(NOW(), INTERVAL 4 DAY) " +
            "    WHEN 4 THEN DATE_ADD(NOW(), INTERVAL 7 DAY) " +
            "    ELSE DATE_ADD(NOW(), INTERVAL 15 DAY) END), " +
            "submit_time = NOW()")
    int upsertCorrect(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("answer") String answer);

    /**
     * 答错分支：continuous_correct / review_level 归零，wrong_count+1，明天再复习。
     */
    @Insert("INSERT INTO user_question_record " +
            "(user_id, question_id, last_answer, is_correct, total_count, wrong_count, continuous_correct, review_level, next_review_time, mastered, submit_time) " +
            "VALUES (#{userId}, #{questionId}, #{answer}, 0, 1, 1, 0, 0, DATE_ADD(NOW(), INTERVAL 1 DAY), 0, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "last_answer = VALUES(last_answer), is_correct = 0, total_count = total_count + 1, " +
            "wrong_count = wrong_count + 1, continuous_correct = 0, review_level = 0, " +
            "mastered = 0, next_review_time = DATE_ADD(NOW(), INTERVAL 1 DAY), submit_time = NOW()")
    int upsertWrong(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("answer") String answer);

    /**
     * 简答题：不判分 is_correct=NULL，仅留存作答（文档 4.3），不改变复习调度状态。
     */
    @Insert("INSERT INTO user_question_record " +
            "(user_id, question_id, last_answer, is_correct, total_count, wrong_count, continuous_correct, review_level, next_review_time, mastered, submit_time) " +
            "VALUES (#{userId}, #{questionId}, #{answer}, NULL, 1, 0, 0, 0, NULL, 0, NOW()) " +
            "ON DUPLICATE KEY UPDATE " +
            "last_answer = VALUES(last_answer), is_correct = NULL, total_count = total_count + 1, submit_time = NOW()")
    int upsertEssay(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("answer") String answer);

    /**
     * 错题本（文档 4.3 SQL 原样条件：deleted=0 AND mastered=0 AND is_correct=0）
     */
    @Select("SELECT r.question_id AS questionId, q.type, q.title AS title, q.category_id AS categoryId, c.name AS categoryName, " +
            "r.last_answer AS lastAnswer, r.total_count AS totalCount, r.wrong_count AS wrongCount, r.submit_time AS submitTime, NULL AS nextReviewTime " +
            "FROM user_question_record r " +
            "LEFT JOIN question q ON r.question_id = q.id AND q.deleted = 0 " +
            "LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE r.user_id = #{userId} AND r.deleted = 0 AND r.mastered = 0 AND r.is_correct = 0 " +
            "ORDER BY r.submit_time DESC")
    IPage<QuestionWrongVO> selectWrongBook(Page<QuestionWrongVO> page, @Param("userId") Long userId);

    /**
     * 复习队列（文档 4.3 SQL 原样条件：deleted=0 AND mastered=0 AND next_review_time <= NOW()，双条件互为兜底）
     */
    @Select("SELECT r.question_id AS questionId, q.type, q.title AS title, q.category_id AS categoryId, c.name AS categoryName, " +
            "r.last_answer AS lastAnswer, r.total_count AS totalCount, r.wrong_count AS wrongCount, r.submit_time AS submitTime, r.next_review_time AS nextReviewTime " +
            "FROM user_question_record r " +
            "LEFT JOIN question q ON r.question_id = q.id AND q.deleted = 0 " +
            "LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE r.user_id = #{userId} AND r.deleted = 0 AND r.mastered = 0 AND r.next_review_time IS NOT NULL AND r.next_review_time <= NOW() " +
            "ORDER BY r.next_review_time ASC")
    IPage<QuestionWrongVO> selectReviewQueue(Page<QuestionWrongVO> page, @Param("userId") Long userId);
}
