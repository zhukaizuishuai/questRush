package com.learn.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.learn.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 用户表 Mapper（文档 3.1）
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 软删用户：同一事务内改写唯一字段再置删除标记（文档 3.1）。
     * username 改写为 __del_{id}；email 截断改写避免超出 VARCHAR(100)。
     */
    @Update("UPDATE `user` SET username = CONCAT('__del_', id), " +
            "email = IF(email IS NULL, NULL, CONCAT('__del_', id, '_', LEFT(email, 60))), " +
            "deleted = 1 WHERE id = #{id}")
    int softDelete(@Param("id") Long id);

    /**
     * 登录失败计数 +1（账号维度，文档 4.7）；累计到 5 次同时锁定 15 分钟。
     */
    @Update("UPDATE `user` SET login_fail_count = login_fail_count + 1, " +
            "login_lock_time = IF(login_fail_count + 1 >= 5, DATE_ADD(NOW(), INTERVAL 15 MINUTE), login_lock_time) " +
            "WHERE id = #{id}")
    int recordLoginFail(@Param("id") Long id);

    @Update("UPDATE `user` SET login_fail_count = 0, login_lock_time = NULL WHERE id = #{id}")
    int resetLoginFail(@Param("id") Long id);

    /**
     * VIP 权益发放：剩余时长顺延而非覆盖（文档 4.5），事务内调用。
     */
    @Update("UPDATE `user` SET vip_expire_time = " +
            "IF(vip_expire_time IS NULL OR vip_expire_time < NOW(), NOW(), vip_expire_time) + INTERVAL #{months} MONTH " +
            "WHERE id = #{userId}")
    int grantVip(@Param("userId") Long userId, @Param("months") int months);

    @Select("SELECT COUNT(*) FROM `user` WHERE deleted = 0 AND vip_expire_time IS NOT NULL AND vip_expire_time >= NOW()")
    long countVipUsers();
}
