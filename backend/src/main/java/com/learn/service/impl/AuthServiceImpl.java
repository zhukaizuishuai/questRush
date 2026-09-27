package com.learn.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.learn.dto.EmailCodeDTO;
import com.learn.dto.LoginDTO;
import com.learn.dto.RegisterDTO;
import com.learn.dto.ResetPasswordDTO;
import com.learn.entity.User;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserMapper;
import com.learn.service.AuthService;
import com.learn.service.MailService;
import com.learn.util.CacheService;
import com.learn.util.IpUtil;
import com.learn.vo.CaptchaVO;
import com.learn.vo.LoginVO;
import com.learn.vo.UserVO;
import com.wf.captcha.SpecCaptcha;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 认证服务实现（文档 4.7 验证码规则全部落在本类）
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Resource
    private UserMapper userMapper;

    @Resource
    private CacheService cache;

    @Resource
    private MailService mailService;

    @Override
    public CaptchaVO captcha(HttpServletRequest request) {
        // IP 限流：同一 IP 每分钟最多 20 次（文档 4.7）
        String ip = IpUtil.getIp(request);
        long count = cache.increment("captcha:ip:" + ip, Duration.ofMinutes(1));
        if (count > 20) {
            throw new BizException(ResultCode.PARAM_ERROR, "获取验证码过于频繁，请稍后再试");
        }
        // easy-captcha：4 位字母数字 + 干扰
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String code = captcha.text().toLowerCase();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        // 存储 captcha:graph:{id}，TTL 5 分钟（文档 4.7）
        cache.set("captcha:graph:" + captchaId, code, Duration.ofMinutes(5));
        return new CaptchaVO(captchaId, captcha.toBase64());
    }

    @Override
    public void verifyGraphCaptcha(String captchaId, String code) {
        if (captchaId == null || captchaId.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "验证码不能为空");
        }
        String key = "captcha:graph:" + captchaId;
        Object stored = cache.get(key);
        // 无论成功还是失败都立即删除，一次性生效（文档 4.7）
        cache.delete(key);
        if (stored == null || code == null || !stored.toString().equalsIgnoreCase(code.trim())) {
            throw new BizException(ResultCode.PARAM_ERROR, "验证码错误或已过期");
        }
    }

    @Override
    public void register(RegisterDTO dto, HttpServletRequest request) {
        // 注册强制图形验证码（文档 4.7）
        verifyGraphCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        // 软删用户的 username 已被改写为 __del_{id}，此处查询天然释放原名（文档 3.1）
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists > 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "用户名已被占用");
        }
        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            Long emailExists = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (emailExists > 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "邮箱已被绑定");
            }
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname() == null || dto.getNickname().isBlank() ? dto.getUsername() : dto.getNickname());
        user.setEmail(dto.getEmail() == null || dto.getEmail().isBlank() ? null : dto.getEmail());
        user.setRole("user");
        user.setStatus(1);
        user.setLoginFailCount(0);
        userMapper.insert(user);
    }

    @Override
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        String ip = IpUtil.getIp(request);
        // IP 维度限流：15 分钟内失败 20 次锁 IP 30 分钟（文档 4.7）
        if (cache.exists("login:ip:lock:" + ip)) {
            throw new BizException(ResultCode.PARAM_ERROR, "操作过于频繁，请 30 分钟后再试");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));

        // 账号锁定检查（账号维度，文档 4.7）
        if (user != null && user.getLoginLockTime() != null && user.getLoginLockTime().isAfter(LocalDateTime.now())) {
            throw new BizException(ResultCode.PARAM_ERROR, "账号已锁定，请 15 分钟后再试");
        }

        // 渐进式验证码：同一账号连续失败 >= 3 次后要求图形验证码（文档 4.7）
        boolean captchaRequired = user != null
                && user.getLoginFailCount() != null
                && user.getLoginFailCount() >= 3;
        if (captchaRequired) {
            verifyGraphCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        }

        // 密码校验：无论账号不存在还是密码错误，提示完全一致，防账号枚举（文档 4.7）
        if (user == null || !encoder.matches(dto.getPassword(), user.getPassword())) {
            if (user != null) {
                userMapper.recordLoginFail(user.getId());
            }
            long ipFails = cache.increment("login:ip:fail:" + ip, Duration.ofMinutes(15));
            if (ipFails >= 20) {
                cache.set("login:ip:lock:" + ip, 1, Duration.ofMinutes(30));
            }
            if (captchaRequired || (user != null && user.getLoginFailCount() + 1 >= 3)) {
                throw new BizException(ResultCode.PARAM_ERROR, "用户名或密码错误",
                        Map.of("requireCaptcha", true));
            }
            throw new BizException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }

        // 禁用账号单独提示（该信息对正常用户必要，且此时密码已校验通过，无枚举风险，文档 4.7）
        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "账号已被禁用，请联系管理员");
        }

        // 登录成功：重置失败计数并签发 Token
        userMapper.resetLoginFail(user.getId());
        StpUtil.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUser(toUserVO(user));
        return vo;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void sendEmailCode(EmailCodeDTO dto, HttpServletRequest request) {
        // 找回密码强制图形验证码（文档 4.7）
        verifyGraphCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        String email = dto.getEmail().trim();
        // 同一邮箱 60 秒内只能发送 1 次（文档 4.7）
        if (!cache.setIfAbsent("mail:cooldown:" + email, 1, Duration.ofSeconds(60))) {
            throw new BizException(ResultCode.PARAM_ERROR, "发送过于频繁，请 60 秒后再试");
        }
        // 同一 IP 每日最多 10 次（文档 4.7）
        String ip = IpUtil.getIp(request);
        if (cache.increment("mail:ip:" + ip, Duration.ofDays(1)) > 10) {
            throw new BizException(ResultCode.PARAM_ERROR, "今日发送次数已达上限");
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        // 邮箱验证码：6 位数字 TTL 10 分钟（文档 4.7）
        cache.set("captcha:mail:" + email, code, Duration.ofMinutes(10));
        mailService.sendCaptcha(email, code);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        String email = dto.getEmail().trim();
        String key = "captcha:mail:" + email;
        Object stored = cache.get(key);
        if (stored == null || !stored.toString().equals(dto.getMailCode() == null ? "" : dto.getMailCode().trim())) {
            throw new BizException(ResultCode.PARAM_ERROR, "邮箱验证码错误或已过期");
        }
        cache.delete(key);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "该邮箱未绑定任何账号");
        }
        User update = new User();
        update.setId(user.getId());
        update.setPassword(encoder.encode(dto.getNewPassword()));
        userMapper.updateById(update);
    }

    private UserVO toUserVO(User user) {
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
