package com.gov.grid.mq;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "GOV_GRID_EVENT_SYNC", consumerGroup = "gov-grid-event-sync-consumer")
public class EventSyncConsumer implements RocketMQListener<String> {

    private final EventService eventService;

    private static final Map<String, Object> PROCESSING_LOCKS = new ConcurrentHashMap<>();

    @Override
    public void onMessage(String message) {
        long startTs = System.currentTimeMillis();
        try {
            JSONObject json = JSONUtil.parseObj(message);
            String action = json.getStr("action");

            log.info("[EventSyncConsumer] 收到消息，action: {}, 耗时等待开始处理", action);

            switch (action) {
                case "BATCH_SYNC":
                    handleBatchSync(json);
                    break;
                case "SINGLE_SYNC":
                    handleSingleSync(json);
                    break;
                default:
                    log.warn("[EventSyncConsumer] 未知的消息动作类型：{}", action);
            }

            log.info("[EventSyncConsumer] 消息处理完成，action: {}, 耗时: {}ms",
                    action, System.currentTimeMillis() - startTs);
        } catch (Exception e) {
            log.error("[EventSyncConsumer] 处理事件同步消息失败，消息内容：{}", message, e);
        }
    }

    private void handleBatchSync(JSONObject json) {
        String batchId = json.getStr("batchId");
        Long userId = json.getLong("userId");

        List<?> rawEvents = json.get("events") instanceof List
                ? (List<?>) json.get("events")
                : new ArrayList<>();

        List<EventReportDTO> events = new ArrayList<>();
        for (Object raw : rawEvents) {
            if (raw instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) raw;
                EventReportDTO dto = BeanUtil.mapToBean(map, EventReportDTO.class, true);
                events.add(dto);
            } else if (raw instanceof JSONObject) {
                EventReportDTO dto = ((JSONObject) raw).toBean(EventReportDTO.class);
                events.add(dto);
            }
        }

        log.info("[EventSyncConsumer] 批量同步处理，batchId: {}, 事件数: {}", batchId, events.size());
        eventService.processBatch(events, userId);
    }

    private void handleSingleSync(JSONObject json) {
        String clientId = json.getStr("clientId");
        Long userId = json.getLong("userId");

        Object eventObj = json.get("event");
        EventReportDTO dto;
        if (eventObj instanceof JSONObject) {
            dto = ((JSONObject) eventObj).toBean(EventReportDTO.class);
        } else if (eventObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) eventObj;
            dto = BeanUtil.mapToBean(map, EventReportDTO.class, true);
        } else {
            log.warn("[EventSyncConsumer] 单条同步消息event字段格式异常，clientId: {}", clientId);
            return;
        }

        if (clientId != null && !clientId.isEmpty()) {
            Object lock = PROCESSING_LOCKS.computeIfAbsent(clientId, k -> new Object());
            synchronized (lock) {
                try {
                    EventInfo existing = eventService.findByClientId(clientId);
                    if (existing != null) {
                        log.info("[EventSyncConsumer] 单条同步跳过重复，clientId: {}, serverId: {}",
                                clientId, existing.getId());
                        return;
                    }
                    eventService.reportEvent(dto, userId, false);
                } finally {
                    PROCESSING_LOCKS.remove(clientId);
                }
            }
        } else {
            eventService.reportEvent(dto, userId, false);
        }
    }
}
