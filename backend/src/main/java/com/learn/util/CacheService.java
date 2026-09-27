package com.learn.util;

import java.time.Duration;

/**
 * 缓存服务抽象：验证码、刷题会话、限流计数等短生命周期数据的统一入口。
 *
 * 简化说明：本项目不依赖 Redis，用 {@link InMemoryCacheService}（ConcurrentHashMap + 过期时间，懒清理）实现。
 * 生产部署时替换为 Redis 实现（set/get/incr/expire/setnx 一一对应）即可，业务代码无需改动（文档 3.0）。
 */
public interface CacheService {

    /** 写入缓存并设置 TTL */
    void set(String key, Object value, Duration ttl);

    /** 读取缓存，不存在或已过期返回 null */
    Object get(String key);

    /** 删除指定 key */
    void delete(String key);

    /** key 是否存在（未过期） */
    boolean exists(String key);

    /** 不存在时才写入（对应 Redis SETNX），返回是否写入成功 */
    boolean setIfAbsent(String key, Object value, Duration ttl);

    /** 计数 +1（对应 Redis INCR），key 首次创建时设置 TTL 窗口，返回累加后的值 */
    long increment(String key, Duration ttl);

    /** 计数 +1 并在达到阈值时刷新 TTL（用于 IP 锁定等场景），返回累加后的值 */
    long incrementAndGet(String key, long threshold, Duration ttlOnReachThreshold);

    /** 获取剩余过期秒数，key 不存在返回 -1 */
    long getTtlSeconds(String key);
}
