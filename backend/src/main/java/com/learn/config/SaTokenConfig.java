package com.learn.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.learn.entity.User;
import com.learn.exception.BizException;
import com.learn.exception.ResultCode;
import com.learn.mapper.UserMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import cn.dev33.satoken.router.SaRouter;

/**
 * Sa-Token 路由拦截配置（文档 4.1 / 5.1）。
 *
 * 1. 登录拦截：除 /api/auth/** 外全部要求登录
 * 2. 角色拦截：/api/admin/** 要求 role = admin，非管理员 403
 * 3. 账号状态校验（易漏项）：Sa-Token 不会因数据库 status 改 0 而自动失效已签发 Token，
 *    每次请求查库校验 status=0 或 deleted=1 时 StpUtil.logout() 并抛 401
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Resource
    private UserMapper userMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. 登录 + 角色拦截
        registry.addInterceptor(new SaInterceptor(handle -> {
                    SaRouter.match("/api/**").notMatch("/api/auth/**")
                            .check(r -> StpUtil.checkLogin());
                    SaRouter.match("/api/admin/**")
                            .check(r -> StpUtil.checkRole("admin"));
                }))
                .addPathPatterns("/api/**");

        // 2. 账号状态校验（在登录拦截之后执行，只有已登录请求才会走到这里）
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!StpUtil.isLogin()) {
                    return true;
                }
                String path = request.getRequestURI();
                if (path.startsWith("/api/auth/")) {
                    return true;
                }
                // 文档 4.1 账号状态校验
                User user = userMapper.selectById(StpUtil.getLoginIdAsLong());
                if (user == null || user.getStatus() == 0 || user.getDeleted() == 1) {
                    StpUtil.logout();
                    throw new BizException(ResultCode.ACCOUNT_DISABLED);
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
