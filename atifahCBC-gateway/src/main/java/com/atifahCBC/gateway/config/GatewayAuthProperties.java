package com.atifahCBC.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置。
 *
 * @author DepeToi
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "atifahCBC.gateway.auth")
public class GatewayAuthProperties {

    /** 是否启用网关鉴权；关闭时过滤器不装配，网关退化为纯转发。 */
    private boolean enabled = false;

    /** 免鉴权路径前缀，按规范化后的路径匹配。 */
    private List<String> whitelist = new ArrayList<>();

    /** 需要管理员角色的路径前缀。 */
    private List<String> adminPaths = new ArrayList<>(List.of("/admin/api/"));

    /** 令牌中承载角色的声明名。 */
    private String roleClaim = "role";

    /** 允许访问管理端接口的角色值。 */
    private String adminRole = "admin";

    /** 允许通过查询参数携带令牌的路径前缀；浏览器 WebSocket 握手无法设置 Authorization 头。 */
    private List<String> websocketPaths = new ArrayList<>(List.of("/ws/"));

    /** WebSocket 握手令牌的查询参数名。 */
    private String tokenQueryParam = "token";
}
