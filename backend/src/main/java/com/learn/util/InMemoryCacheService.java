package com.learn.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的 CacheService 实现（简化方案，生产可换 Redis 实现）。
 *
 * - 数据结构：ConcurrentHashMap&lt;String, Entry&gt;，Entry 含 value 与过期时间戳
 * - 过期策略：懒清理（读取时判断 + 每隔一段写操作批量清扫），无后台线程
 * - 计数：AtomicLong 保证并发安全
 */
@Component
public class InMemoryCacheService implements CacheService {

    private static class Entry {
        final Object value;
        final long expireAtMillis;

        Entry(Object value, long expireAtMillis) {
            this.value = value;
            this.expireAtMillis = expireAtMillis;
        }

        boolean expired() {
            return System.currentTimeMillis() >= expireAtMillis;
        }
    }

    /** 计数器专用 Entry：AtomicLong + 过期时间 */
    private static class CounterEntry {
        final AtomicLong counter = new AtomicLong(0);
        volatile long expireAtMillis;

        CounterEntry(long expireAtMillis) {
            this.expireAtMillis = expireAtMillis;
        }

        boolean expired() {
            return System.currentTimeMillis() >= expireAtMillis;
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final Map<String, CounterEntry> counters = new ConcurrentHashMap<>();

    /** 每累计多少次写操作做一次全量懒清扫 */
    private static final int CLEAN_BATCH = 256;
    private final AtomicLong writeCount = new AtomicLong(0);

    private void maybeClean() {
        if (writeCount.incrementAndGet() % CLEAN_BATCH == 0) {
            cleanExpired();
        }
    }

    private synchronized void cleanExpired() {
        Iterator<Map.Entry<String, Entry>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expired()) {
                it.remove();
            }
        }
        Iterator<Map.Entry<String, CounterEntry>> cit = counters.entrySet().iterator();
        while (cit.hasNext()) {
            if (cit.next().getValue().expired()) {
                cit.remove();
            }
        }
    }

    @Override
    public void set(String key, Object value, Duration ttl) {
        store.put(key, new Entry(value, System.currentTimeMillis() + ttl.toMillis()));
        maybeClean();
    }

    @Override
    public Object get(String key) {
        Entry e = store.get(key);
        if (e == null) {
            return null;
        }
        if (e.expired()) {
            store.remove(key);
            return null;
        }
        return e.value;
    }

    @Override
    public void delete(String key) {
        store.remove(key);
        counters.remove(key);
    }

    @Override
    public boolean exists(String key) {
        return get(key) != null;
    }

    @Override
    public boolean setIfAbsent(String key, Object value, Duration ttl) {
        boolean absent = !exists(key);
        if (absent) {
            set(key, value, ttl);
        }
        return absent;
    }

    @Override
    public long increment(String key, Duration ttl) {
        return incrementAndGet(key, -1, ttl);
    }

    @Override
    public long incrementAndGet(String key, long threshold, Duration ttlOnReachThreshold) {
        long now = System.currentTimeMillis();
        CounterEntry entry = counters.compute(key, (k, old) -> {
            if (old == null || old.expired()) {
                // 新窗口：过期时间先设为 +ttl，达到阈值时再按调用方要求覆盖
                return new CounterEntry(now + ttlOnReachThreshold.toMillis());
            }
            return old;
        });
        long v = entry.counter.incrementAndGet();
        if (threshold > 0 && v >= threshold) {
            entry.expireAtMillis = now + ttlOnReachThreshold.toMillis();
        }
        maybeClean();
        return v;
    }

    @Override
    public long getTtlSeconds(String key) {
        Entry e = store.get(key);
        if (e == null || e.expired()) {
            return -1;
        }
        return Math.max(0, (e.expireAtMillis - System.currentTimeMillis()) / 1000);
    }
}
