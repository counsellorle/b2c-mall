package com.lxs.b2cmall.shop.auth;

/**
 * C 端登录态存储。抽象成接口，便于单测替换为内存实现，生产走 Redis。
 */
public interface UserTokenStore {

    /** 保存 token → userId 映射，ttlSeconds 后自动失效 */
    void save(String token, Integer userId, long ttlSeconds);

    /** 根据 token 取 userId，不存在或已过期返回 null */
    Integer findUserId(String token);

    /** 删除 token（登出） */
    void remove(String token);
}
