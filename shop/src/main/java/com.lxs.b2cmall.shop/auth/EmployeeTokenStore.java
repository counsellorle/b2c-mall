package com.lxs.b2cmall.shop.auth;

/**
 * B 端（商家后台）登录态存储。
 * <p>
 * 与 C 端 {@link UserTokenStore} 分开存放，Redis key 前缀不同，避免两种身份混用。
 * 生产实现见 {@link RedisEmployeeTokenStore}，单测可替换为内存实现。
 */
public interface EmployeeTokenStore {

    /**
     * 保存 token → 店铺/员工映射，ttlSeconds 后自动失效。
     */
    void save(String token, Integer shopId, Integer employeeId, long ttlSeconds);

    /**
     * 根据 token 取会话，不存在或已过期返回 null。
     */
    EmployeeSession find(String token);

    /**
     * 删除 token（退出登录）。
     */
    void remove(String token);

    /**
     * B 端会话内容：所属店铺 + 员工。
     */
    class EmployeeSession {
        private final Integer shopId;
        private final Integer employeeId;

        public EmployeeSession(Integer shopId, Integer employeeId) {
            this.shopId = shopId;
            this.employeeId = employeeId;
        }

        public Integer getShopId() {
            return shopId;
        }

        public Integer getEmployeeId() {
            return employeeId;
        }
    }
}
