package com.atifahCBC.gateway.filter;

import com.atifahCBC.common.core.constant.CommonConstants;
import com.atifahCBC.common.core.enums.ErrorCode;
import com.atifahCBC.common.core.result.Result;
import com.atifahCBC.common.security.jwt.JwtTokenProvider;
import com.atifahCBC.gateway.config.GatewayAuthProperties;
import com.atifahCBC.gateway.constant.GatewayConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

/**
 * JWT 鉴权全局过滤器：校验令牌，写入下游身份头，并对管理端接口校验角色。
 *
 * @author DepeToi
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "atifahCBC.gateway.auth", name = "enabled", havingValue = "true")
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** Bearer 令牌前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** 用户 ID 安全字符与长度限制，避免日志注入与非法下游头。 */
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private final JwtTokenProvider jwtTokenProvider;
    private final GatewayAuthProperties authProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String rawPath = request.getURI().getRawPath() == null ? "-" : request.getURI().getRawPath();

        String path;
        try {
            path = normalizePath(rawPath);
        } catch (IllegalArgumentException ex) {
            // 非法编码或越界路径直接拒绝，避免用 ../ 绕过白名单
            return reject(exchange, rawPath, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "invalid_path");
        }

        if (matchesAny(path, authProperties.getWhitelist())) {
            return chain.filter(exchange);
        }

        ResolvedToken resolved = resolveToken(request, path);
        if (resolved == null) {
            return reject(exchange, path, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "missing_token");
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(resolved.value());
        } catch (Exception ex) {
            return reject(exchange, path, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    resolved.fromQuery() ? "invalid_ws_token" : "invalid_token");
        }

        String userId = claims.getSubject();
        if (userId == null || !USER_ID_PATTERN.matcher(userId).matches()) {
            return reject(exchange, path, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "invalid_token");
        }

        // 先记录用户 ID，管理端越权被拒时访问日志也能定位到人
        exchange.getAttributes().put(GatewayConstants.ATTR_USER_ID, userId);

        if (matchesAny(path, authProperties.getAdminPaths()) && !hasAdminRole(claims)) {
            return reject(exchange, path, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "forbidden_role");
        }

        ServerHttpRequest.Builder requestBuilder = request.mutate()
                .header(CommonConstants.HEADER_USER_ID, userId);
        Object username = claims.get("username");
        if (username == null) {
            requestBuilder.headers(headers -> headers.remove(CommonConstants.HEADER_USERNAME));
        } else {
            requestBuilder.header(CommonConstants.HEADER_USERNAME, String.valueOf(username));
        }
        if (resolved.fromQuery()) {
            // 握手令牌只用于网关校验，不透传给下游，避免进入下游日志
            requestBuilder.uri(removeTokenParam(request.getURI()));
        }

        ServerWebExchange mutatedExchange = exchange.mutate().request(requestBuilder.build()).build();
        return chain.filter(mutatedExchange);
    }

    /**
     * 规范化路径：解码百分号编码并消解 "." 与 ".." 段，越界时抛异常。
     *
     * @param rawPath 请求原始路径
     * @return 规范化后的绝对路径
     */
    private String normalizePath(String rawPath) {
        String decoded = UriUtils.decode(rawPath, StandardCharsets.UTF_8);
        Deque<String> segments = new ArrayDeque<>();
        for (String segment : decoded.split("/")) {
            if (segment.isEmpty() || ".".equals(segment)) {
                continue;
            }
            if ("..".equals(segment)) {
                if (segments.isEmpty()) {
                    throw new IllegalArgumentException("path traversal");
                }
                segments.removeLast();
                continue;
            }
            segments.addLast(segment);
        }
        return "/" + String.join("/", segments);
    }

    /**
     * 从 Authorization 头或 WebSocket 查询参数中取出令牌。
     *
     * @param request 请求
     * @param path 规范化后的路径
     * @return 令牌与来源；未携带时返回 null
     */
    private ResolvedToken resolveToken(ServerHttpRequest request, String path) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            return token.isEmpty() ? null : new ResolvedToken(token, false);
        }
        if (matchesAny(path, authProperties.getWebsocketPaths())) {
            String token = request.getQueryParams().getFirst(authProperties.getTokenQueryParam());
            return StringUtils.hasText(token) ? new ResolvedToken(token, true) : null;
        }
        return null;
    }

    /**
     * 判断路径是否命中任一前缀，前缀按路径段边界匹配，避免 /authx 误命中 /auth。
     *
     * @param path 规范化后的路径
     * @param prefixes 配置的前缀列表
     * @return 是否命中
     */
    private boolean matchesAny(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(prefix -> matchesPrefix(path, prefix));
    }

    private boolean matchesPrefix(String path, String prefix) {
        String trimmed = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;
        return path.equals(trimmed) || path.startsWith(trimmed + "/");
    }

    /**
     * 校验角色声明；同时兼容字符串与数组两种写法。
     *
     * @param claims 令牌声明
     * @return 是否具备管理员角色
     */
    private boolean hasAdminRole(Claims claims) {
        Object role = claims.get(authProperties.getRoleClaim());
        if (role == null) {
            return false;
        }
        if (role instanceof Collection<?> roles) {
            return roles.contains(authProperties.getAdminRole());
        }
        return authProperties.getAdminRole().equals(String.valueOf(role));
    }

    /**
     * 去掉 WebSocket 握手请求上的令牌参数，其余参数保持不变。
     *
     * @param uri 原始请求 URI
     * @return 去掉令牌后的 URI
     */
    private URI removeTokenParam(URI uri) {
        return UriComponentsBuilder.fromUri(uri)
                .replaceQueryParam(authProperties.getTokenQueryParam())
                .build(true)
                .toUri();
    }

    /**
     * 返回统一结构的失败响应，并记录不含令牌的失败原因。
     *
     * @param exchange 请求交换上下文
     * @param path 请求路径
     * @param status HTTP 状态
     * @param errorCode 业务错误码
     * @param reason 失败原因
     * @return 响应流
     */
    private Mono<Void> reject(ServerWebExchange exchange, String path, HttpStatus status,
                              ErrorCode errorCode, String reason) {
        logRejected(exchange, path, status, reason);

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(serialize(errorCode));
        return response.writeWith(Mono.just(buffer));
    }

    /**
     * 序列化统一错误体；序列化异常时退化为最小结构，保证错误码不丢失。
     *
     * @param errorCode 业务错误码
     * @return 响应字节
     */
    private byte[] serialize(ErrorCode errorCode) {
        try {
            return objectMapper.writeValueAsBytes(Result.fail(errorCode));
        } catch (JsonProcessingException ex) {
            log.error("event=auth_response_build_failed", ex);
            return ("{\"code\":" + errorCode.getCode() + ",\"message\":\"" + errorCode.getMessage() + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * 记录鉴权失败；只包含路径、状态与原因，不记录令牌与 claims。
     *
     * @param exchange 请求交换上下文
     * @param path 请求路径
     * @param status HTTP 状态
     * @param reason 失败原因
     */
    private void logRejected(ServerWebExchange exchange, String path, HttpStatus status, String reason) {
        String traceId = exchange.getAttribute(GatewayConstants.ATTR_TRACE_ID);
        try (MDC.MDCCloseable ignored = MDC.putCloseable(CommonConstants.MDC_TRACE_ID,
                traceId == null ? "-" : traceId)) {
            log.warn("event=auth_failed path={} status={} reason={}", path, status.value(), reason);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 令牌及其来源：请求头，或 WebSocket 握手的查询参数。
     *
     * @param value 令牌原文
     * @param fromQuery 是否来自查询参数
     */
    private record ResolvedToken(String value, boolean fromQuery) {
    }
}
