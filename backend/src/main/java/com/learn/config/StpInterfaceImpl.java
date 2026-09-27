package com.learn.config;

import cn.dev33.satoken.stp.StpInterface;
import com.learn.entity.User;
import com.learn.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色数据源：从 user 表实时读取角色（admin / user）。
 * 实时读取而非登录时缓存，保证管理员被禁用 / 改角色后立即生效。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Resource
    private UserMapper userMapper;

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        User user = userMapper.selectById(Long.valueOf(loginId.toString()));
        if (user == null || user.getDeleted() == 1) {
            return Collections.emptyList();
        }
        return "admin".equals(user.getRole()) ? List.of("admin") : Collections.emptyList();
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 本项目角色即权限，不使用细粒度权限点
        return Collections.emptyList();
    }
}
