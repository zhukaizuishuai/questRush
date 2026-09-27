package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.QuestionOption;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 题目选项表 Mapper（文档 3.4）。判断题选项为 A.正确 / B.错误。
 */
@Mapper
public interface QuestionOptionMapper extends BaseMapper<QuestionOption> {

    @Select("SELECT * FROM question_option WHERE question_id = #{questionId} AND deleted = 0 " +
            "ORDER BY sort ASC, option_code ASC")
    List<QuestionOption> selectByQuestionId(@Param("questionId") Long questionId);

    @Select("<script>" +
            "SELECT * FROM question_option WHERE deleted = 0 AND question_id IN " +
            "<foreach item='i' collection='questionIds' open='(' separator=',' close=')'>#{i}</foreach> " +
            "ORDER BY question_id ASC, sort ASC, option_code ASC" +
            "</script>")
    List<QuestionOption> selectByQuestionIds(@Param("questionIds") List<Long> questionIds);
}
