package com.atifahCBC.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关限流配置，规则按路由 ID 生效。
 *
 * @author DepeToi
 * @since 1.0.0
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "atifahCBC.gateway.rate-limit")
public class GatewayRateLimitProperties {

    /** 是否启用限流；规则为空时不产生任何限制。 */
    private boolean enabled = false;

    /** 限流规则列表。 */
    private List<Rule> rules = new ArrayList<>();

    /**
     * 单条限流规则。
     */
    @Getter
    @Setter
    public static class Rule {

        /** 网关路由 ID，例如 atifahCBC-mood。 */
        private String resource;

        /** 每秒允许的请求数。 */
        private double count;

        /** 突发容量，为空时与 count 相同。 */
        private Integer burst;
    }
}
