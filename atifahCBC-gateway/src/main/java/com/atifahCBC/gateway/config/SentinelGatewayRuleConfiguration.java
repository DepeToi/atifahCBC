package com.atifahCBC.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 把配置中的限流规则装载到 Sentinel 网关规则管理器。
 * 默认关闭；只有显式打开并配置规则时才产生限流行为。
 *
 * @author DepeToi
 * @since 1.0.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
@ConditionalOnProperty(prefix = "atifahCBC.gateway.rate-limit", name = "enabled", havingValue = "true")
public class SentinelGatewayRuleConfiguration {

    private final GatewayRateLimitProperties properties;

    /**
     * 启动时装载规则；规则为空时不做任何限制。
     */
    @PostConstruct
    void loadRules() {
        List<GatewayFlowRule> rules = toFlowRules(properties.getRules());
        GatewayRuleManager.loadRules(new HashSet<>(rules));
        log.info("event=gateway_rate_limit_loaded rules={}", rules.size());
    }

    /**
     * 把配置项转换为 Sentinel 网关流控规则，跳过缺少资源名或阈值的条目。
     *
     * @param rules 配置规则
     * @return Sentinel 规则列表
     */
    static List<GatewayFlowRule> toFlowRules(List<GatewayRateLimitProperties.Rule> rules) {
        List<GatewayFlowRule> flowRules = new ArrayList<>();
        for (GatewayRateLimitProperties.Rule rule : rules) {
            if (!StringUtils.hasText(rule.getResource()) || rule.getCount() <= 0) {
                continue;
            }
            GatewayFlowRule flowRule = new GatewayFlowRule(rule.getResource()).setCount(rule.getCount());
            if (rule.getBurst() != null && rule.getBurst() > 0) {
                flowRule.setBurst(rule.getBurst());
            }
            flowRules.add(flowRule);
        }
        return flowRules;
    }
}
