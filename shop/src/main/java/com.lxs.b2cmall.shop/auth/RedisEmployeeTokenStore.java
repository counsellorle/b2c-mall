package com.lxs.b2cmall.shop.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 B 端登录态存储：key = b2c:employee:token:{token}，value = shopId:employeeId。
 * <p>
 * 使用 {@link StringRedisTemplate} 保证值是纯文本，网关侧可直接用字符串读取校验。
 */
@Component
public class RedisEmployeeTokenStore implements EmployeeTokenStore {

    public static final String KEY_PREFIX = "b2c:employee:token:";

    private static final String SEPARATOR = ":";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void save(String token, Integer shopId, Integer employeeId, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + token,
                shopId + SEPARATOR + employeeId, ttlSeconds, TimeUnit.SECONDS);
    }

    @Override
    public EmployeeSession find(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String value = stringRedisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (value == null || value.isEmpty()) {
            return null;
        }
        String[] parts = value.split(SEPARATOR);
        if (parts.length < 1) {
            return null;
        }
        try {
            Integer shopId = Integer.valueOf(parts[0]);
            Integer employeeId = parts.length > 1 ? Integer.valueOf(parts[1]) : null;
            return new EmployeeSession(shopId, employeeId);
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
