package com.atifahCBC.common.mq.constant;

/**
 * RocketMQ 消费组统一命名，避免各环境冲突。
 *
 * @author DepeToi
 * @since 1.0.0
 */
public final class MqConsumerGroups {

    /** 统计服务-情绪事件消费组。 */
    public static final String STATS_MOOD_CONSUMER = "atifahCBC-stats-mood-consumer";

    /** 分析服务-情绪事件消费组。 */
    public static final String ANALYSIS_MOOD_CONSUMER = "atifahCBC-analysis-mood-consumer";

    /** 成长服务-情绪事件消费组。 */
    public static final String GROWTH_MOOD_CONSUMER = "atifahCBC-growth-mood-consumer";

    /** 消息服务-通知消费组。 */
    public static final String MESSAGE_NOTIFICATION_CONSUMER = "atifahCBC-message-notification-consumer";

    /** 社区服务-扇出消费组。 */
    public static final String COMMUNITY_FANOUT_CONSUMER = "atifahCBC-community-fanout-consumer";

    /** 内容服务-审核结果消费组。 */
    public static final String AUDIT_RESULT_CONSUMER = "atifahCBC-audit-result-consumer";

    private MqConsumerGroups() {
    }
}
