package com.learn.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 提取：优先取反向代理传递的 X-Forwarded-For 首段，退回 remoteAddr。
 */
public final class IpUtil {

    private IpUtil() {
    }

    public static String getIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // 多级代理时第一个 IP 为客户端真实 IP
            int idx = xff.indexOf(',');
            return (idx > 0 ? xff.substring(0, idx) : xff).trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
