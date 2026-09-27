package com.learn.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.learn.dto.AdminUserStatusDTO;
import com.learn.dto.AdminUserVipDTO;
import com.learn.entity.User;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserMapper;
import com.learn.service.AdminUserService;
import com.learn.util.PageUtil;
import com.learn.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 管理端用户服务实现（文档 3.1 / 6.4）
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public IPage<UserVO> page(long pageNum, long pageSize, String keyword, Integer status, String role) {
        Page<User> page = new Page<>(PageUtil.num(pageNum), PageUtil.size(pageSize));
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getUsername, keyword)
                        .or().like(User::getNickname, keyword))
                .eq(status != null, User::getStatus, status)
                .eq(role != null && !role.isBlank(), User::getRole, role)
                .orderByDesc(User::getId);
        return userMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public void updateStatus(AdminUserStatusDTO dto) {
        User target = userMapper.selectById(dto.getUserId());
        if (target == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (dto.getStatus() == 0 && "admin".equals(target.getRole())) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能禁用管理员账号");
        }
        User update = new User();
        update.setId(dto.getUserId());
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);
        // 禁用时强制登出（文档 6.4）；启用无需处理
        if (dto.getStatus() == 0) {
            StpUtil.logout(dto.getUserId());
        }
    }

    @Override
    public void updateVip(AdminUserVipDTO dto) {
        if (userMapper.selectById(dto.getUserId()) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 直接覆盖（文档 4.5）。必须用 UpdateWrapper 显式 set：
        // MyBatis-Plus 默认非空策略会跳过 null 字段，用 updateById 无法把 vipExpireTime 置空（取消 VIP）。
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getVipExpireTime, dto.getVipExpireTime())
                .eq(User::getId, dto.getUserId()));
    }

    @Override
    public void delete(Long id) {
        if (id.equals(StpUtil.getLoginIdAsLong())) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能删除自己");
        }
        if (userMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        // 软删：username=CONCAT('__del_',id)、email 截断改写后置 deleted=1（文档 3.1）
        userMapper.softDelete(id);
        StpUtil.logout(id);
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setVipExpireTime(user.getVipExpireTime());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
