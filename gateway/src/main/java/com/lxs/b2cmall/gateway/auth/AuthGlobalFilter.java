package com.lxs.b2cmall.gateway.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一登录鉴权网关过滤器。
 * <p>
 * 职责：
 * <ol>
 *   <li>白名单（登录/注册/C 端商品浏览）直接放行</li>
 *   <li>B 端路径校验员工会话（Redis key b2c:employee:token:*，value shopId:employeeId）</li>
 *   <li>C 端路径校验用户会话（Redis key b2c:user:token:*，value userId）</li>
 *   <li>校验通过后把身份写入内部请求头（X-Shop-Id / X-Employee-Id / X-User-Id），供 shop-server 使用</li>
 *   <li>无论是否白名单，一律剥离客户端自带身份头，防止伪造越权</li>
 * </ol>
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 身份头：客户端不允许自带，统一由网关写入 */
    private static final String HEADER_AUTH = "Authorization";
    private static final String HEADER_USER_TOKEN = "X-User-Token";
    private static final String HEADER_EMPLOYEE_TOKEN = "X-Employee-Token";
    private static final String BEARER = "bearer ";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final ReactiveStringRedisTemplate redis;
    private final AuthProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthGlobalFilter(ReactiveStringRedisTemplate redis, AuthProperties props) {
        this.redis = redis;
        this.props = props;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 先清理客户端伪造的身份头，白名单请求同样处理
        ServerWebExchange cleaned = stripIdentity(exchange);
        ServerHttpRequest request = cleaned.getRequest();
        String path = request.getURI().getPath();

        if (HttpMethod.OPTIONS.equals(request.getMethod())
                || isWhitelisted(path)
                || !isProtected(path)) {
            return chain.filter(cleaned);
        }

        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            return reject(cleaned, 401, "请先登录");
        }

        boolean backend = isBackend(path);
        String key = (backend ? props.getEmployeeTokenKeyPrefix() : props.getUserTokenKeyPrefix()) + token;

        Mono<String> session = redis.opsForValue().get(key)
                .onErrorMap(e -> {
                    log.error("读取登录态失败, key={}", key, e);
                    return new AuthException(503, "鉴权服务暂不可用，请稍后重试");
                });

        return session
                .switchIfEmpty(Mono.error(new AuthException(401, "登录已过期，请重新登录")))
                .flatMap(value -> {
                    ServerWebExchange injected = backend
                            ? injectBackend(cleaned, value)
                            : injectFrontend(cleaned, value);
                    if (injected == null) {
                        return reject(cleaned, 401, "登录状态异常，请重新登录");
                    }
                    return chain.filter(injected);
                })
                .onErrorResume(AuthException.class, e -> reject(cleaned, e.getStatus(), e.getMessage()));
    }

    /** 剥离客户端自带的身份头 */
    private ServerWebExchange stripIdentity(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(props.getUserIdHeader());
                    headers.remove(props.getShopIdHeader());
                    headers.remove(props.getEmployeeIdHeader());
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    /** 注入 C 端用户身份，sessionValue 异常时返回 null */
    private ServerWebExchange injectFrontend(ServerWebExchange exchange, String sessionValue) {
        String userId = sessionValue == null ? "" : sessionValue.trim();
        if (!isNumeric(userId)) {
            return null;
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(props.getUserIdHeader(), userId)
                .build();
        return exchange.mutate().request(request).build();
    }

    /** 注入 B 端店铺/员工身份，sessionValue 需为 shopId:employeeId */
    private ServerWebExchange injectBackend(ServerWebExchange exchange, String sessionValue) {
        if (!StringUtils.hasText(sessionValue)) {
            return null;
        }
        String[] parts = sessionValue.split(":");
        if (parts.length < 1 || !isNumeric(parts[0])) {
            return null;
        }
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header(props.getShopIdHeader(), parts[0]);
        if (parts.length > 1 && isNumeric(parts[1])) {
            builder.header(props.getEmployeeIdHeader(), parts[1]);
        }
        return exchange.mutate().request(builder.build()).build();
    }

    /** 提取 token：Authorization: Bearer → X-Employee-Token → X-User-Token */
    private String resolveToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HEADER_AUTH);
        if (StringUtils.hasText(auth)) {
            String trimmed = auth.trim();
            if (trimmed.length() > BEARER.length()
                    && trimmed.substring(0, BEARER.length()).toLowerCase().equals(BEARER)) {
                return trimmed.substring(BEARER.length()).trim();
            }
            return trimmed;
        }
        String employeeToken = request.getHeaders().getFirst(HEADER_EMPLOYEE_TOKEN);
        if (StringUtils.hasText(employeeToken)) {
            return employeeToken.trim();
        }
        String userToken = request.getHeaders().getFirst(HEADER_USER_TOKEN);
        return StringUtils.hasText(userToken) ? userToken.trim() : null;
    }

    private boolean isWhitelisted(String path) {
        List<String> whitelist = props.getWhitelist();
        if (whitelist == null) {
            return false;
        }
        for (String pattern : whitelist) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBackend(String path) {
        return matchesAny(props.getBackendPrefixes(), path);
    }

    private boolean isProtected(String path) {
        return isBackend(path) || matchesAny(props.getFrontendPrefixes(), path);
    }

    private boolean matchesAny(List<String> prefixes, String path) {
        if (prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isNumeric(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** 统一 JSON 错误响应，结构与 shop-server 的 Result 保持一致 */
    private Mono<Void> reject(ServerWebExchange exchange, int status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.valueOf(status));
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", status);
        body.put("message", message);
        body.put("data", null);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"code\":" + status + ",\"message\":\"" + message + "\",\"data\":null}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        // 高于路由过滤器，保证在转发前完成鉴权
        return -100;
    }

    /** 鉴权异常，携带 HTTP 状态与提示文案 */
    static class AuthException extends RuntimeException {
        private final int status;

        AuthException(int status, String message) {
            super(message);
            this.status = status;
        }

        int getStatus() {
            return status;
        }
    }
}
