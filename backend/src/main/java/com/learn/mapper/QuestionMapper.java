package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.entity.Question;
import com.learn.vo.QuestionAdminVO;
import com.learn.vo.QuestionListVO;
import com.learn.vo.QuestionPracticeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 题目表 Mapper（文档 3.3）。
 * 注意：答题前置场景的查询（selectPracticeById / selectReadableIds）不 select answer/answer_text/analysis 三列（文档 4.2）。
 */
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {

    /**
     * 前台题目列表 / 搜索（文档 4.1：SQL 层按权限过滤 is_vip）。
     * vipFilter = true 时只查免费题（权限优先，忽略前端传入的 isVip）；
     * 返回 QuestionListVO（不含答案字段）。
     */
    @Select("<script>" +
            "SELECT q.id, q.category_id AS categoryId, c.name AS categoryName, q.type, q.difficulty, " +
            "q.is_vip AS isVip, q.like_count AS likeCount, q.status, q.title, q.create_time AS createTime " +
            "FROM question q LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE q.deleted = 0 AND q.status = 1 " +
            "<if test='categoryIds != null'> AND q.category_id IN " +
            "<foreach item='i' collection='categoryIds' open='(' separator=',' close=')'>#{i}</foreach></if>" +
            "<if test='keyword != null and keyword != \"\"'> AND q.title LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='type != null'> AND q.type = #{type}</if>" +
            "<if test='difficulty != null'> AND q.difficulty = #{difficulty}</if>" +
            "<if test='vipFilter'> AND q.is_vip = 0</if>" +
            "<if test='!vipFilter and isVip != null'> AND q.is_vip = #{isVip}</if>" +
            " ORDER BY q.id DESC" +
            "</script>")
    IPage<QuestionListVO> selectPageList(Page<QuestionListVO> page,
                                         @Param("categoryIds") List<Long> categoryIds,
                                         @Param("keyword") String keyword,
                                         @Param("type") Integer type,
                                         @Param("difficulty") Integer difficulty,
                                         @Param("isVip") Integer isVip,
                                         @Param("vipFilter") boolean vipFilter);

    /**
     * 刷题会话候选题目 id 列表（文档 4.3）：顺序模式按 id 排序，随机模式由 Java shuffle。
     * 同样不查答案三列。
     */
    @Select("<script>" +
            "SELECT q.id FROM question q WHERE q.deleted = 0 AND q.status = 1 " +
            "<if test='categoryIds != null'> AND q.category_id IN " +
            "<foreach item='i' collection='categoryIds' open='(' separator=',' close=')'>#{i}</foreach></if>" +
            "<if test='difficulty != null'> AND q.difficulty = #{difficulty}</if>" +
            "<if test='vipFilter'> AND q.is_vip = 0</if>" +
            " ORDER BY q.id" +
            "</script>")
    List<Long> selectReadableIds(@Param("categoryIds") List<Long> categoryIds,
                                 @Param("difficulty") Integer difficulty,
                                 @Param("vipFilter") boolean vipFilter);

    /**
     * 答题前详情（QuestionPracticeVO，不含 answer/answerText/analysis，文档 4.2）
     */
    @Select("SELECT q.id, q.category_id AS categoryId, c.name AS categoryName, q.type, q.difficulty, " +
            "q.is_vip AS isVip, q.title " +
            "FROM question q LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE q.id = #{id} AND q.deleted = 0")
    QuestionPracticeVO selectPracticeById(@Param("id") Long id);

    /**
     * 管理端题目分页（QuestionAdminVO，全字段含答案，仅 admin 可调用）
     */
    @Select("<script>" +
            "SELECT q.*, c.name AS categoryName FROM question q " +
            "LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE q.deleted = 0 " +
            "<if test='categoryIds != null'> AND q.category_id IN " +
            "<foreach item='i' collection='categoryIds' open='(' separator=',' close=')'>#{i}</foreach></if>" +
            "<if test='keyword != null and keyword != \"\"'> AND q.title LIKE CONCAT('%', #{keyword}, '%')</if>" +
            "<if test='type != null'> AND q.type = #{type}</if>" +
            "<if test='isVip != null'> AND q.is_vip = #{isVip}</if>" +
            "<if test='difficulty != null'> AND q.difficulty = #{difficulty}</if>" +
            "<if test='status != null'> AND q.status = #{status}</if>" +
            " ORDER BY q.id DESC" +
            "</script>")
    IPage<QuestionAdminVO> selectAdminPage(Page<QuestionAdminVO> page,
                                           @Param("categoryIds") List<Long> categoryIds,
                                           @Param("keyword") String keyword,
                                           @Param("type") Integer type,
                                           @Param("isVip") Integer isVip,
                                           @Param("difficulty") Integer difficulty,
                                           @Param("status") Integer status);

    /**
     * 管理端题目详情（含答案字段）
     */
    @Select("SELECT q.*, c.name AS categoryName FROM question q " +
            "LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE q.id = #{id} AND q.deleted = 0")
    QuestionAdminVO selectAdminById(@Param("id") Long id);

    /**
     * 点赞计数原子增减，不允许减成负数（文档 4.6 第 5 步）
     */
    @Update("UPDATE question SET like_count = like_count + #{delta} WHERE id = #{id} AND like_count + #{delta} >= 0")
    int changeLikeCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * 按点赞表重算 like_count 兜底接口用（文档 4.6）。只重算未删除题目。
     */
    @Update("UPDATE question q SET like_count = " +
            "(SELECT COUNT(*) FROM question_like l WHERE l.question_id = q.id AND l.deleted = 0) " +
            "WHERE q.deleted = 0")
    int recalcLikeCount();

    /**
     * 只取点赞数（供答题前详情使用，避免为拿 likeCount 而全字段查出答案列，文档 4.2 精神）
     */
    @Select("SELECT like_count FROM question WHERE id = #{id} AND deleted = 0")
    Integer selectLikeCount(@Param("id") Long id);

    /**
     * 导入查重：按 (category_id, title_md5) 批量查询已存在题目（文档 4.4）
     */
    @Select("<script>" +
            "SELECT * FROM question WHERE deleted = 0 AND category_id IN " +
            "<foreach item='i' collection='categoryIds' open='(' separator=',' close=')'>#{i}</foreach> " +
            "AND title_md5 IN " +
            "<foreach item='m' collection='md5s' open='(' separator=',' close=')'>#{m}</foreach>" +
            "</script>")
    List<Question> selectByCategoryAndMd5(@Param("categoryIds") List<Long> categoryIds,
                                          @Param("md5s") List<String> md5s);
}
