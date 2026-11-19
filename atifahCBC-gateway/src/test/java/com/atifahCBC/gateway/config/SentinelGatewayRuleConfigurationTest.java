package com.atifahCBC.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 限流规则转换测试：正常规则、突发容量与无效条目过滤。
 *
 * @author DepeToi
 * @since 1.0.0
 */
class SentinelGatewayRuleConfigurationTest {

    @Test
    void convertsConfiguredRulesAndSkipsInvalidOnes() {
        GatewayRateLimitProperties.Rule valid = rule("atifahCBC-mood", 20, 40);
        GatewayRateLimitProperties.Rule noResource = rule(" ", 10, null);
        GatewayRateLimitProperties.Rule noCount = rule("atifahCBC-community", 0, null);

        List<GatewayFlowRule> rules = SentinelGatewayRuleConfiguration
                .toFlowRules(List.of(valid, noResource, noCount));

        assertEquals(1, rules.size());
        assertEquals("atifahCBC-mood", rules.get(0).getResource());
        assertEquals(20d, rules.get(0).getCount());
        assertEquals(40, rules.get(0).getBurst());
    }

    @Test
    void returnsEmptyWhenNoRulesConfigured() {
        assertTrue(SentinelGatewayRuleConfiguration.toFlowRules(List.of()).isEmpty());
    }

    private GatewayRateLimitProperties.Rule rule(String resource, double count, Integer burst) {
        GatewayRateLimitProperties.Rule rule = new GatewayRateLimitProperties.Rule();
        rule.setResource(resource);
        rule.setCount(count);
        rule.setBurst(burst);
        return rule;
    }
}
