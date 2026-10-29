package com.lxs.b2cmall.gateway.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 网关鉴权配置（application.yml 中 b2c.auth.*）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "b2c.auth")
public class AuthProperties {

    /** 免登录白名单，支持 Ant 风格（如 /front/product/**） */
    private List<String> whitelist = new ArrayList<>(Arrays.asList(
            "/employee/login",
            "/employee/register",
            "/user/login",
            "/user/register",
            "/front/products",
            "/front/categories",
            "/front/product/**",
            "/actuator/**"
    ));

    /** B 端（商家后台）路径前缀：校验员工会话，并注入 shopId / employeeId */
    private List<String> backendPrefixes = new ArrayList<>(Arrays.asList(
            "/employee/", "/dashboard/", "/product/", "/order/"
    ));

    /** C 端（买家）路径前缀：校验用户会话，并注入 userId */
    private List<String> frontendPrefixes = new ArrayList<>(Arrays.asList(
            "/user/", "/front/order/", "/cart/", "/pay/"
    ));

    /** C 端 token 在 Redis 中的 key 前缀，value 为 userId */
    private String userTokenKeyPrefix = "b2c:user:token:";

    /** B 端 token 在 Redis 中的 key 前缀，value 为 shopId:employeeId */
    private String employeeTokenKeyPrefix = "b2c:employee:token:";

    private String userIdHeader = "X-User-Id";

    private String shopIdHeader = "X-Shop-Id";

    private String employeeIdHeader = "X-Employee-Id";
}
