package com.lxs.b2cmall.shop.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * 从请求中解析当前登录的 C 端用户。
 * <p>
 * 支持两种携带方式：{@code Authorization: Bearer {token}} 与 {@code X-User-Token: {token}}。
 * P5 阶段在 shop 服务内校验；P6 接入网关后由网关统一校验并注入用户身份，此处逻辑可直接复用。
 */
@Component
public class CurrentUser {

    public static final String HEADER_AUTH = "Authorization";
    public static final String HEADER_TOKEN = "X-User-Token";
    private static final String BEARER = "bearer ";

    @Autowired
    private UserTokenStore tokenStore;

    /** 取当前用户 id，未登录/已过期直接抛异常，由 Controller 统一转为业务错误 */
    public Integer requireUserId(HttpServletRequest request) {
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
