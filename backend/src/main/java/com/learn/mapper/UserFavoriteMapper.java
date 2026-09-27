package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.entity.UserFavorite;
import com.learn.vo.FavoriteItemVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户收藏表 Mapper（文档 3.7）。
 * 关键点：唯一键不含 deleted，toggle 必须查到含 deleted=1 的行再翻转，
 * 因此 selectAnyRow 不走 MP 的逻辑删除过滤（原生 SQL）。
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {

    /** 查含已软删在内的行（toggle 依赖） */
    @Select("SELECT * FROM user_favorite WHERE user_id = #{userId} AND question_id = #{questionId}")
    UserFavorite selectAnyRow(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /**
     * 原子 toggle：行不存在则插入 deleted=0（收藏）；已存在则就地翻转 deleted。
     * 由唯一键 uk_user_question 保证单行，并发首次收藏不会撞键（对比先 SELECT 后 INSERT 的竞态）。
     */
    @Insert("INSERT INTO user_favorite (user_id, question_id, deleted) VALUES (#{userId}, #{questionId}, 0) " +
            "ON DUPLICATE KEY UPDATE deleted = IF(deleted = 0, 1, 0)")
    int upsertToggle(@Param("userId") Long userId, @Param("questionId") Long questionId);

    /** 翻转 deleted（文档 3.0：toggle 语义，永不新增行——行已存在时） */
    @Update("UPDATE user_favorite SET deleted = #{deleted} WHERE id = #{id}")
    int updateDeletedById(@Param("id") Long id, @Param("deleted") Integer deleted);

    /** 收藏列表（join 题目与分类） */
    @Select("SELECT f.question_id AS questionId, q.type, q.difficulty, q.is_vip AS isVip, q.title AS title, " +
            "q.category_id AS categoryId, c.name AS categoryName, f.update_time AS favoritedTime " +
            "FROM user_favorite f " +
            "LEFT JOIN question q ON f.question_id = q.id AND q.deleted = 0 " +
            "LEFT JOIN question_category c ON q.category_id = c.id AND c.deleted = 0 " +
            "WHERE f.user_id = #{userId} AND f.deleted = 0 " +
            "ORDER BY f.update_time DESC")
    IPage<FavoriteItemVO> selectFavoritePage(Page<FavoriteItemVO> page, @Param("userId") Long userId);
}
