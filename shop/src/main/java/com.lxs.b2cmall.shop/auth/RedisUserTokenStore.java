package com.lxs.b2cmall.shop.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的登录态存储：key = b2c:user:token:{token}，value = userId。
 * <p>
 * 使用 {@link StringRedisTemplate} 保证值是纯文本（"3" 而不是带类型的 JSON），
 * 网关 AuthGlobalFilter 用字符串方式读取时才能直接解析。
 */
@Component
public class RedisUserTokenStore implements UserTokenStore {

    private static final String KEY_PREFIX = "b2c:user:token:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String token, Integer userId, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + token, String.valueOf(userId),
                ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Integer findUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void remove(String token) {
        if (token != null && !token.isEmpty()) {
            stringRedisTemplate.delete(KEY_PREFIX + token);
        }
    }
}
