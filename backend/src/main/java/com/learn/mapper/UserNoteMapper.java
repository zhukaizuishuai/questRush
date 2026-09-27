package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.entity.UserNote;
import com.learn.vo.NoteVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户笔记表 Mapper（文档 3.8）。唯一键不含 deleted，保存必须走 upsert 复用同一行。
 */
@Mapper
public interface UserNoteMapper extends BaseMapper<UserNote> {

    /**
     * 文档 3.8 原样 SQL：再次保存已软删的笔记会把 deleted 置回 0。
     */
    @Insert("INSERT INTO user_note (user_id, question_id, content) VALUES (#{userId}, #{questionId}, #{content}) " +
            "ON DUPLICATE KEY UPDATE content = VALUES(content), deleted = 0")
    int upsertNote(@Param("userId") Long userId, @Param("questionId") Long questionId, @Param("content") String content);

    /** 笔记列表（join 题目取题干；仅本人、未软删，按更新时间倒序） */
    @Select("SELECT n.id, n.question_id AS questionId, q.title AS questionTitle, n.content, " +
            "n.create_time AS createTime, n.update_time AS updateTime " +
            "FROM user_note n " +
            "LEFT JOIN question q ON n.question_id = q.id AND q.deleted = 0 AND q.status = 1 " +
            "WHERE n.user_id = #{userId} AND n.deleted = 0 " +
            "ORDER BY n.update_time DESC")
    IPage<NoteVO> selectNotePage(Page<NoteVO> page, @Param("userId") Long userId);
}
