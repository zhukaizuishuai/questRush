package com.learn.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户表（文档 3.1）
 * 唯一键 uk_username / uk_email 不含 deleted；软删时由 SQL 改写唯一字段（见 UserMapper.softDelete）
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** BCrypt 加密密码（60 字符） */
    private String password;

    private String nickname;

    private String avatar;

    /** 邮箱，未绑定为 NULL（唯一索引允许多个 NULL） */
    private String email;

    /** admin / user */
    private String role;

    /** VIP 过期时间，NULL = 从未开通 */
    private LocalDateTime vipExpireTime;

    /** 1 正常 0 禁用 */
    private Integer status;

    /** 连续登录失败次数 */
    private Integer loginFailCount;

    /** 锁定截止时间 */
    private LocalDateTime loginLockTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
