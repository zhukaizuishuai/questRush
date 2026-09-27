package com.learn.controller.admin;

import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.learn.common.PageResult;
import com.learn.common.Result;
import com.learn.dto.AdminUserStatusDTO;
import com.learn.dto.AdminUserVipDTO;
import com.learn.service.AdminUserService;
import com.learn.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理（文档 6.4）。/api/admin/** 由 SaTokenConfig 统一校验 admin 角色。
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    /** 用户列表 */
    @GetMapping("/list")
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                           @RequestParam(defaultValue = "10") long pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) String role) {
        IPage<UserVO> page = adminUserService.page(pageNum, pageSize, keyword, status, role);
        return Result.ok(PageResult.of(page, page.getRecords()));
    }

    /** 禁用 / 启用，并强制登出 */
    @PutMapping("/status")
    public Result<Void> updateStatus(@Valid @RequestBody AdminUserStatusDTO dto) {
        adminUserService.updateStatus(dto);
        return Result.ok();
    }

    /** 手动设置 VIP 过期时间（直接覆盖，文档 4.5） */
    @PutMapping("/vip")
    public Result<Void> updateVip(@Valid @RequestBody AdminUserVipDTO dto) {
        adminUserService.updateVip(dto);
        return Result.ok();
    }

    /** 删除用户（软删，改写唯一字段释放 username/email，文档 3.1） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return Result.ok();
    }
}
