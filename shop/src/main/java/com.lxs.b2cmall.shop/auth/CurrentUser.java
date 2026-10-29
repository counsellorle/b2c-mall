package com.lxs.b2cmall.shop.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 从请求中解析当前登录的 C 端用户。
 * <p>
 * 优先读取网关注入的 {@code X-User-Id}（P6 起，token 已在网关校验过，无需再查 Redis）；
 * 直连 shop-server 调试时回退为校验 {@code Authorization: Bearer {token}} 或 {@code X-User-Token}。
 */
@Component
public class CurrentUser {

    public static final String HEADER_AUTH = "Authorization";
    public static final String HEADER_TOKEN = "X-User-Token";
    public static final String HEADER_USER_ID = "X-User-Id";
    private static final String BEARER = "bearer ";

    @Autowired
    private UserTokenStore tokenStore;

    /** 取当前用户 id，未登录/已过期直接抛异常，由 Controller 统一转为业务错误 */
    public Integer requireUserId(HttpServletRequest request) {
        Integer injected = userId(request);
        if (injected != null) {
            return injected;
        }
        String token = token(request);
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("请先登录");
        }
        Integer userId = tokenStore.findUserId(token);
        if (userId == null) {
            throw new RuntimeException("登录已过期，请重新登录");
        }
        return userId;
    }

    /** 取网关注入的用户 id，无则返回 null */
    public Integer userId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String value = request.getHeader(HEADER_USER_ID);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 提取请求中的 token（无则返回 null） */
    public String token(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String auth = request.getHeader(HEADER_AUTH);
        if (auth != null) {
            String trimmed = auth.trim();
            if (trimmed.length() > BEARER.length()
                    && trimmed.substring(0, BEARER.length()).toLowerCase().equals(BEARER)) {
                return trimmed.substring(BEARER.length()).trim();
            }
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        String header = request.getHeader(HEADER_TOKEN);
        return header == null ? null : header.trim();
    }
}
