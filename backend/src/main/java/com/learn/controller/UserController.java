package com.learn.controller;

import jakarta.validation.Valid;
import cn.dev33.satoken.stp.StpUtil;
import com.learn.common.Result;
import com.learn.dto.PasswordChangeDTO;
import com.learn.dto.UserUpdateDTO;
import com.learn.service.UserService;
import com.learn.vo.UserVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心接口（文档 6.1）
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    /** 个人信息，含 VIP 状态与到期时间 */
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.info(StpUtil.getLoginIdAsLong()));
    }

    /** 修改昵称、邮箱 */
    @PutMapping("/update")
    public Result<Void> update(@Valid @RequestBody UserUpdateDTO dto) {
        userService.update(StpUtil.getLoginIdAsLong(), dto);
        return Result.ok();
    }

    /** 修改密码，需校验原密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordChangeDTO dto) {
        userService.changePassword(StpUtil.getLoginIdAsLong(), dto);
        return Result.ok();
    }

    /** 头像上传（<=2MB，jpg/png/webp） */
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return Result.ok(userService.uploadAvatar(StpUtil.getLoginIdAsLong(), file));
    }
}
