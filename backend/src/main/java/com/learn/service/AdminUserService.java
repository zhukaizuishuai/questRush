package com.learn.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.dto.AdminUserStatusDTO;
import com.learn.dto.AdminUserVipDTO;
import com.learn.vo.UserVO;

/**
 * 管理端用户服务（文档 6.4）
 */
public interface AdminUserService {

    IPage<UserVO> page(long pageNum, long pageSize, String keyword, Integer status, String role);

    /** 禁用 / 启用，并强制登出 */
    void updateStatus(AdminUserStatusDTO dto);

    /** 手动设置 VIP 过期时间（直接覆盖，文档 4.5） */
    void updateVip(AdminUserVipDTO dto);

    /** 软删用户：改写唯一字段释放 username/email（文档 3.1） */
    void delete(Long id);
}
