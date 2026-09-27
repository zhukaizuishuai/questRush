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
        String username = dto.getUsername() == null ? "" : dto.getUsername().trim();

        // IP 维度限流：15 分钟内失败 20 次锁 IP 30 分钟（文档 4.7）
        if (cache.exists("login:ip:lock:" + ip)) {
            throw new BizException(ResultCode.PARAM_ERROR, "操作过于频繁，请 30 分钟后再试");
        }
        // 账号维度限流改为「按用户名」计数：不区分账号是否存在。
        // 若沿用 DB 的 login_fail_count（只有真实存在的账号才有行），不存在的用户名永远不会触发
        // requireCaptcha / 锁定，攻击者可据此枚举已注册账号（违反文档 4.7 无账号枚举）。
        String userFailKey = "login:user:fail:" + username;
        String userLockKey = "login:user:lock:" + username;
        if (cache.exists(userLockKey)) {
            throw new BizException(ResultCode.PARAM_ERROR, "登录失败次数过多，请 15 分钟后再试");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        // DB 中的锁定状态（管理员改库等场景）；文案与缓存锁定保持一致，避免差异暴露账号是否存在
        if (user != null && user.getLoginLockTime() != null && user.getLoginLockTime().isAfter(LocalDateTime.now())) {
            cache.set(userLockKey, 1, Duration.ofMinutes(15));
            throw new BizException(ResultCode.PARAM_ERROR, "登录失败次数过多，请 15 分钟后再试");
        }

        // 验证码策略（产品调整 2026-09-27）：登录页始终显示验证码，因此「带了就校验」；
        // 渐进式语义：同一用户名连续失败 >= 3 次起，即使不传也强制校验（文档 4.7）
        boolean captchaRequired = readFailCount(userFailKey) >= 3;
        boolean captchaProvided = dto.getCaptchaId() != null && !dto.getCaptchaId().isBlank()
                && dto.getCaptchaCode() != null && !dto.getCaptchaCode().isBlank();
        if (captchaRequired && !captchaProvided) {
            // 与密码错误文案完全一致，仅附加 requireCaptcha 供前端展示输入框
            throw new BizException(ResultCode.PARAM_ERROR, "用户名或密码错误", Map.of("requireCaptcha", true));
        }
        if (captchaProvided) {
            verifyGraphCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        }

        // 密码校验：无论账号不存在还是密码错误，提示完全一致，防账号枚举（文档 4.7）
        boolean passwordOk = user != null && encoder.matches(dto.getPassword(), user.getPassword());
        if (!passwordOk) {
            if (user != null) {
                userMapper.recordLoginFail(user.getId());
            }
            int userFails = readFailCount(userFailKey) + 1;
            cache.set(userFailKey, userFails, Duration.ofMinutes(15));
            long ipFails = cache.increment("login:ip:fail:" + ip, Duration.ofMinutes(15));
            if (ipFails >= 20) {
                cache.set("login:ip:lock:" + ip, 1, Duration.ofMinutes(30));
            }
            if (userFails >= 5) {
                cache.set(userLockKey, 1, Duration.ofMinutes(15));
            }
            if (userFails >= 3) {
                throw new BizException(ResultCode.PARAM_ERROR, "用户名或密码错误", Map.of("requireCaptcha", true));
            }
            throw new BizException(ResultCode.PARAM_ERROR, "用户名或密码错误");
        }

        // 禁用账号单独提示（该信息对正常用户必要，且此时密码已校验通过，无枚举风险，文档 4.7）
        if (user.getStatus() == 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "账号已被禁用，请联系管理员");
        }

        // 登录成功：重置失败计数并签发 Token
        userMapper.resetLoginFail(user.getId());
        cache.delete(userFailKey);
        StpUtil.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setUser(toUserVO(user));
        return vo;
    }

    /** 读取用户名维度的失败计数（CacheService 的计数器不落在 store 上，这里用 get/set 维护） */
    private int readFailCount(String key) {
        Object v = cache.get(key);
        return v instanceof Number n ? n.intValue() : 0;
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
