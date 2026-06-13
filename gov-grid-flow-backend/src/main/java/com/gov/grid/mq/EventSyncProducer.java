package com.gov.grid.mq;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSyncProducer {

    public static final String TOPIC = "GOV_GRID_EVENT_SYNC";

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.group:gov-grid-flow-producer-group}")
    private String producerGroup;

    public void sendBatchSync(List<Map<String, Object>> events, Long userId, String deviceId, String batchId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "BATCH_SYNC");
            message.put("batchId", batchId);
            message.put("userId", userId);
            message.put("deviceId", deviceId);
            message.put("events", events);
            message.put("timestamp", System.currentTimeMillis());

            String payload = JSONUtil.toJsonStr(message);
            rocketMQTemplate.send(TOPIC, MessageBuilder.withPayload(payload).build());

            log.info("[EventSyncProducer] 批量同步消息已发送，batchId: {}, 事件数: {}", batchId, events.size());
        } catch (Exception e) {
            log.error("[EventSyncProducer] 批量同步消息发送失败，batchId: {}", batchId, e);
            throw new RuntimeException("消息队列发送失败: " + e.getMessage(), e);
        }
    }

    public void sendSingleSync(Map<String, Object> event, Long userId, String clientId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("action", "SINGLE_SYNC");
            message.put("clientId", clientId);
            message.put("userId", userId);
            message.put("event", event);
            message.put("timestamp", System.currentTimeMillis());

            String payload = JSONUtil.toJsonStr(message);
            rocketMQTemplate.send(TOPIC, MessageBuilder.withPayload(payload).build());

            log.info("[EventSyncProducer] 单条同步消息已发送，clientId: {}", clientId);
        } catch (Exception e) {
            log.error("[EventSyncProducer] 单条同步消息发送失败，clientId: {}", clientId, e);
            throw new RuntimeException("消息队列发送失败: " + e.getMessage(), e);
        }
    }
}
