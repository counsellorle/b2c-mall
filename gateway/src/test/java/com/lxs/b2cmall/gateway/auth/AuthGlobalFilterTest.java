package com.lxs.b2cmall.gateway.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 网关鉴权过滤器单元测试：白名单、未登录拦截、会话校验、身份注入、伪造头剥离。
 */
class AuthGlobalFilterTest {

    private ReactiveStringRedisTemplate redis;
    private ReactiveValueOperations<String, String> valueOps;
    private AuthProperties props;
    private AuthGlobalFilter filter;

    /** 记录透传到下游的请求 */
    private final AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(ReactiveStringRedisTemplate.class);
        valueOps = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        props = new AuthProperties();
        filter = new AuthGlobalFilter(redis, props);
        captured.set(null);
    }

    private MockServerWebExchange exchange(String path, String... headers) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path);
        for (int i = 0; i + 1 < headers.length; i += 2) {
            builder.header(headers[i], headers[i + 1]);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private String body(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }

    @Test
    @DisplayName("白名单接口无需登录直接放行")
    void whitelistPassesThrough() {
        MockServerWebExchange ex = exchange("/front/products");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        assertThat(captured.get()).isNotNull();
        assertThat(ex.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("无 token 访问受保护接口返回 401")
    void missingTokenReturns401() {
        MockServerWebExchange ex = exchange("/product/page");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        assertThat(captured.get()).isNull();
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(body(ex)).contains("请先登录").contains("\"code\":401");
    }

    @Test
    @DisplayName("token 不存在或已过期返回 401")
    void expiredTokenReturns401() {
        when(valueOps.get("b2c:user:token:expired")).thenReturn(Mono.empty());
        MockServerWebExchange ex = exchange("/cart/list", "X-User-Token", "expired");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        assertThat(captured.get()).isNull();
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(body(ex)).contains("登录已过期");
    }

    @Test
    @DisplayName("B 端 token 校验通过后注入 shopId 与 employeeId")
    void backendTokenInjectsShopAndEmployee() {
        when(valueOps.get("b2c:employee:token:emp-token")).thenReturn(Mono.just("2:7"));
        MockServerWebExchange ex = exchange("/product/page", "Authorization", "Bearer emp-token");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        ServerWebExchange forwarded = captured.get();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Shop-Id")).isEqualTo("2");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Employee-Id")).isEqualTo("7");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    @DisplayName("C 端 token 校验通过后注入 userId")
    void frontendTokenInjectsUserId() {
        when(valueOps.get("b2c:user:token:user-token")).thenReturn(Mono.just("3"));
        MockServerWebExchange ex = exchange("/cart/list", "X-User-Token", "user-token");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        ServerWebExchange forwarded = captured.get();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("3");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-Shop-Id")).isNull();
    }

    @Test
    @DisplayName("客户端伪造的身份头会被剥离")
    void forgedIdentityHeadersAreStripped() {
        when(valueOps.get("b2c:employee:token:emp-token")).thenReturn(Mono.just("2:7"));
        MockServerWebExchange ex = exchange("/order/page",
                "Authorization", "Bearer emp-token",
                "X-Shop-Id", "999",
                "X-User-Id", "888");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        ServerWebExchange forwarded = captured.get();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getRequest().getHeaders().get("X-Shop-Id")).containsExactly("2");
        assertThat(forwarded.getRequest().getHeaders().getFirst("X-User-Id")).isNull();
    }

    @Test
    @DisplayName("Redis 不可用时返回 503，不误放行")
    void redisFailureReturns503() {
        when(valueOps.get(anyString())).thenReturn(Mono.error(new IllegalStateException("connect refused")));
        MockServerWebExchange ex = exchange("/dashboard/summary", "Authorization", "Bearer emp-token");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        assertThat(captured.get()).isNull();
        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(body(ex)).contains("鉴权服务暂不可用");
    }

    @Test
    @DisplayName("非受保护路径（如未知接口）不在网关拦截范围内")
    void unrelatedPathPassesThrough() {
        MockServerWebExchange ex = exchange("/unknown/path");

        filter.filter(ex, e -> {
            captured.set(e);
            return Mono.empty();
        }).block();

        assertThat(captured.get()).isNotNull();
    }
}
