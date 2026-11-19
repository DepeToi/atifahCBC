package com.atifahCBC.gateway.filter;

import com.atifahCBC.common.core.constant.CommonConstants;
import com.atifahCBC.common.security.config.JwtProperties;
import com.atifahCBC.common.security.jwt.JwtTokenProvider;
import com.atifahCBC.gateway.config.GatewayAuthProperties;
import com.atifahCBC.gateway.constant.GatewayConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 网关鉴权测试：白名单、令牌校验、身份头覆盖、路径穿越与管理端角色。
 *
 * @author DepeToi
 * @since 1.0.0
 */
class AuthGlobalFilterTest {

    private static final long USER_ID = 42L;

    private static final String USERNAME = "138****5678";

    private static final String SECRET = "atifahCBC-test-secret-please-change-32bytes";

    private JwtTokenProvider tokenProvider;

    private GatewayAuthProperties authProperties;

    private AuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(jwtProperties(Duration.ofMinutes(15)));
        authProperties = new GatewayAuthProperties();
        authProperties.setWhitelist(List.of("/api/v1/auth/", "/actuator/health"));
        filter = new AuthGlobalFilter(tokenProvider, authProperties, new ObjectMapper());
    }

    @Test
    void allowsWhitelistedPathWithoutToken() {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        MockServerWebExchange exchange = send(request("/api/v1/auth/login"), captured);

        assertNotNull(captured.get(), "白名单路径应直接放行");
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsMissingToken() {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        MockServerWebExchange exchange = send(request("/api/v1/moods"), captured);

        assertNull(captured.get());
        assertUnauthorized(exchange);
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenProvider expiredProvider = new JwtTokenProvider(jwtProperties(Duration.ofSeconds(-1)));
        AuthGlobalFilter expiredFilter = new AuthGlobalFilter(expiredProvider, authProperties, new ObjectMapper());
        String expired = expiredProvider.generateToken(USER_ID, USERNAME, "user");
        MockServerWebExchange exchange = MockServerWebExchange.from(request("/api/v1/moods")
                .header("Authorization", "Bearer " + expired)
                .build());

        expiredFilter.filter(exchange, ex -> Mono.empty()).block();

        assertUnauthorized(exchange);
    }

    @Test
    void writesIdentityHeadersFromToken() {
        String token = tokenProvider.generateToken(USER_ID, USERNAME, "user");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        send(request("/api/v1/moods").header("Authorization", "Bearer " + token), captured);

        ServerWebExchange downstream = captured.get();
        assertNotNull(downstream);
        assertEquals(String.valueOf(USER_ID),
                downstream.getRequest().getHeaders().getFirst(CommonConstants.HEADER_USER_ID));
        assertEquals(USERNAME,
                downstream.getRequest().getHeaders().getFirst(CommonConstants.HEADER_USERNAME));
        assertEquals(String.valueOf(USER_ID), downstream.getAttribute(GatewayConstants.ATTR_USER_ID));
    }

    @Test
    void overwritesSpoofedIdentityHeader() {
        String token = tokenProvider.generateToken(USER_ID, USERNAME, "user");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        send(request("/api/v1/moods")
                .header(CommonConstants.HEADER_USER_ID, "999")
                .header("Authorization", "Bearer " + token), captured);

        assertEquals("42", captured.get().getRequest().getHeaders().getFirst(CommonConstants.HEADER_USER_ID));
    }

    @Test
    void rejectsWhitelistBypassByTraversal() {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        MockServerWebExchange exchange = send(request("/api/v1/auth/../admin/api/users"), captured);

        assertNull(captured.get(), "路径穿越不得命中白名单");
        assertUnauthorized(exchange);
    }

    @Test
    void rejectsAdminPathForUserRole() {
        String token = tokenProvider.generateToken(USER_ID, USERNAME, "user");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        MockServerWebExchange exchange = send(request("/admin/api/users")
                .header("Authorization", "Bearer " + token), captured);

        assertNull(captured.get());
        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body != null && body.contains("40300"), "应返回统一结构的无权限错误体");
    }

    @Test
    void allowsAdminPathForAdminRole() {
        String token = tokenProvider.generateToken(USER_ID, USERNAME, "admin");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        send(request("/admin/api/users").header("Authorization", "Bearer " + token), captured);

        assertNotNull(captured.get());
    }

    @Test
    void acceptsWebsocketTokenFromQueryAndStripsIt() {
        String token = tokenProvider.generateToken(USER_ID, USERNAME, "user");
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        send(request("/ws/chat?token=" + token + "&room=1"), captured);

        ServerWebExchange downstream = captured.get();
        assertNotNull(downstream, "WebSocket 握手应允许通过查询参数传令牌");
        assertNull(downstream.getRequest().getQueryParams().getFirst("token"));
        assertEquals("1", downstream.getRequest().getQueryParams().getFirst("room"));
        assertEquals(String.valueOf(USER_ID),
                downstream.getRequest().getHeaders().getFirst(CommonConstants.HEADER_USER_ID));
    }

    @Test
    void rejectsWebsocketHandshakeWithoutToken() {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        MockServerWebExchange exchange = send(request("/ws/chat"), captured);

        assertNull(captured.get());
        assertUnauthorized(exchange);
    }

    private JwtProperties jwtProperties(Duration ttl) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenTtl(ttl);
        return properties;
    }

    private MockServerHttpRequest.BaseBuilder<?> request(String path) {
        return MockServerHttpRequest.get(path);
    }

    private MockServerWebExchange send(MockServerHttpRequest.BaseBuilder<?> requestBuilder,
                                       AtomicReference<ServerWebExchange> captured) {
        MockServerWebExchange exchange = MockServerWebExchange.from(requestBuilder.build());
        filter.filter(exchange, ex -> {
            captured.set(ex);
            return Mono.empty();
        }).block();
        return exchange;
    }

    private void assertUnauthorized(MockServerWebExchange exchange) {
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body != null && body.contains("40100"), "应返回统一结构的未认证错误体");
    }
}
