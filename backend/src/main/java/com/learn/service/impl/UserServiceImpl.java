package com.learn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.PasswordChangeDTO;
import com.learn.dto.UserUpdateDTO;
import com.learn.entity.User;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserMapper;
import com.learn.service.UserService;
import com.learn.vo.UserVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 个人中心服务实现
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    @Value("${learn.upload-dir}")
    private String uploadDir;

    @Override
    public UserVO info(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
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

    @Override
    public void update(Long userId, UserUpdateDTO dto) {
        User update = new User();
        update.setId(userId);
        update.setNickname(dto.getNickname());
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            // 邮箱唯一校验（排除本人）
            Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, dto.getEmail())
                    .ne(User::getId, userId));
            if (exists > 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "该邮箱已被其他账号绑定");
            }
            update.setEmail(dto.getEmail());
        }
        userMapper.updateById(update);
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null || !encoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR, "原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    @Override
    public String uploadAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "请选择头像文件");
        }
        // <= 2MB，jpg/png/webp（文档 6.1）
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BizException(ResultCode.PARAM_ERROR, "头像不能超过 2MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException(ResultCode.PARAM_ERROR, "仅支持 jpg/png/webp 格式");
        }
        try {
            // 文件名重命名为 UUID（上传安全，文档 4.4）
            Path dir = Paths.get(uploadDir, "avatar");
            Files.createDirectories(dir);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(filename).toFile());
            String url = "/uploads/avatar/" + filename;
            User update = new User();
            update.setId(userId);
            update.setAvatar(url);
            userMapper.updateById(update);
            return url;
        } catch (IOException e) {
            log.error("avatar upload failed", e);
            throw new BizException(ResultCode.SERVER_ERROR);
        }
    }
}
