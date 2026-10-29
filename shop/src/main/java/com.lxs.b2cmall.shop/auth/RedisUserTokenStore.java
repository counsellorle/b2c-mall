package com.lxs.b2cmall.shop.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的登录态存储：key = b2c:user:token:{token}，value = userId。
 */
@Component
public class RedisUserTokenStore implements UserTokenStore {

    private static final String KEY_PREFIX = "b2c:user:token:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(String token, Integer userId, long ttlSeconds) {
        redisTemplate.opsForValue().set(KEY_PREFIX + token, userId, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Integer findUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        Object value = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void remove(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.delete(KEY_PREFIX + token);
        }
    }
}
