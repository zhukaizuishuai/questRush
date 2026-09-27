package com.learn.service;

import com.learn.dto.PasswordChangeDTO;
import com.learn.dto.UserUpdateDTO;
import com.learn.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 个人中心服务（文档 6.1）
 */
public interface UserService {

    UserVO info(Long userId);

    void update(Long userId, UserUpdateDTO dto);

    void changePassword(Long userId, PasswordChangeDTO dto);

    /** 头像上传（<= 2MB，jpg/png/webp），返回访问 URL */
    String uploadAvatar(Long userId, MultipartFile file);
}
