package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.QuestionLike;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 题目点赞表 Mapper（文档 3.10 / 4.6）。五步事务的 SQL 全部落在这里：
 * 1. 占位 upsert（确保行存在并持排他行锁，避免 SELECT FOR UPDATE 的 gap lock 死锁）
 * 2. SELECT ... FOR UPDATE 当前读（RR 下普通 SELECT 是快照读，读不到并发事务的最新值）
 * 3. delta 由 service 层基于第 2 步结果推导（MySQL 的 upsert 不返回行值）
 * 4. UPDATE 最终状态
 * 5. like_count 原子增减在 QuestionMapper.changeLikeCount（含 >=0 保护）
 */
@Mapper
public interface QuestionLikeMapper extends BaseMapper<QuestionLike> {

    /** 第 1 步：占位 upsert。行不存在插入 deleted=1；行已存在则空更新，同样加排他锁。 */
    @Insert("INSERT INTO question_like (user_id, question_id, deleted) VALUES (#{userId}, #{questionId}, 1) " +
            "ON DUPLICATE KEY UPDATE deleted = deleted")
    int upsertPlaceholder(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 第 2 步：当前读。此时行必然存在，取 deleted 判定本次 toggle 方向。 */
    @Select("SELECT deleted FROM question_like WHERE user_id = #{userId} AND question_id = #{questionId} FOR UPDATE")
    Integer selectDeletedForUpdate(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 第 4 步：写入最终状态。 */
    @Update("UPDATE question_like SET deleted = #{deleted} WHERE user_id = #{userId} AND question_id = #{questionId}")
    int updateDeleted(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("deleted") Integer deleted);

    /** 用户当前是否已点赞（非事务查询，详情页展示用） */
    @Select("SELECT COUNT(*) FROM question_like WHERE user_id = #{userId} AND question_id = #{questionId} AND deleted = 0")
    long countLiked(@Param("userId") Long userId, @Param("questionId") Long questionId);
}
