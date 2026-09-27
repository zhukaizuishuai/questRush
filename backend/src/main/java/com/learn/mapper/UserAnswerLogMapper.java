package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.UserAnswerLog;
import com.learn.vo.CategoryStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 用户作答流水表 Mapper（文档 3.6）。只追加，统计口径：正确率 = 正确条数 / 总作答条数。
 */
@Mapper
public interface UserAnswerLogMapper extends BaseMapper<UserAnswerLog> {

    @Select("SELECT COUNT(*) FROM user_answer_log WHERE user_id = #{userId}")
    long countTotal(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_answer_log WHERE user_id = #{userId} AND is_correct = 1")
    long countCorrect(@Param("userId") Long userId);

    @Select("SELECT q.category_id AS categoryId, IFNULL(c.name, '未分类') AS categoryName, COUNT(*) AS total, " +
            "SUM(IF(l.is_correct = 1, 1, 0)) AS correct " +
            "FROM user_answer_log l " +
            "LEFT JOIN question q ON l.question_id = q.id AND q.deleted = 0 " +
            "LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE l.user_id = #{userId} " +
            "GROUP BY q.category_id, IFNULL(c.name, '未分类') " +
            // 排除关联不到题目的行（题目被物理删除/软删），否则会产生 categoryId=null 的脏分组
            "HAVING categoryId IS NOT NULL")
    List<CategoryStatVO> selectCategoryStats(@Param("userId") Long userId);

    /** 最近 N 天有作答记录的日期（倒序），服务连续打卡天数计算 */
    @Select("SELECT DISTINCT DATE(submit_time) FROM user_answer_log WHERE user_id = #{userId} ORDER BY 1 DESC LIMIT 90")
    List<LocalDate> selectRecentDates(@Param("userId") Long userId);

    /** 日均答题量 = 总作答数 / 覆盖天数（管理端统计） */
    @Select("SELECT COUNT(*) / GREATEST(DATEDIFF(NOW(), MIN(submit_time)) + 1, 1) FROM user_answer_log")
    Double selectDailyAvg();
}
