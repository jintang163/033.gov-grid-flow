package com.gov.grid.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.BatchSyncResponseDTO;
import com.gov.grid.dto.EventQueryDTO;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventProcess;
import com.gov.grid.enums.EventPriority;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.ProcessAction;
import com.gov.grid.mapper.EventEvaluationMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.EventProcessMapper;
import com.gov.grid.mq.EventSyncProducer;
import com.gov.grid.security.DataScopeUtils;
import com.gov.grid.service.EventService;
import com.gov.grid.service.EventUrgeService;
import com.gov.grid.service.ImageComparisonService;
import com.gov.grid.workflow.WorkflowService;
import com.gov.grid.vo.EventDetailVO;
import com.gov.grid.vo.WarningInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String EVENT_NO_KEY = "event:no:";
    private static final String EVENT_NO_LOCK_KEY = "event:no:lock";
    private static final long EVENT_NO_LOCK_EXPIRE = 10;

    private static final String CLIENT_ID_CACHE_PREFIX = "event:clientId:";

    private final EventInfoMapper eventInfoMapper;
    private final EventProcessMapper eventProcessMapper;
    private final EventEvaluationMapper eventEvaluationMapper;
    private final WorkflowService workflowService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ImageComparisonService imageComparisonService;
    private final EventSyncProducer eventSyncProducer;
    private final EventUrgeService eventUrgeService;

    @Override
    public EventInfo reportEvent(EventReportDTO dto, Long userId) {
        return reportEvent(dto, userId, false);
    }

    @Override
    public EventInfo reportEvent(EventReportDTO dto, Long userId, boolean async) {
        if (async) {
            String clientId = dto.getClientId() != null ? dto.getClientId() : IdUtil.fastSimpleUUID();
            Map<String, Object> eventMap = BeanUtil.beanToMap(dto);
            eventSyncProducer.sendSingleSync(eventMap, userId, clientId);

            EventInfo placeholder = new EventInfo();
            placeholder.setClientId(clientId);
            placeholder.setTitle(dto.getTitle());
            placeholder.setStatus("QUEUED");
            log.info("[EventService] 单条事件已投递MQ异步处理，clientId: {}", clientId);
            return placeholder;
        }

        if (StrUtil.isNotBlank(dto.getClientId())) {
            EventInfo existing = findByClientId(dto.getClientId());
            if (existing != null) {
                log.info("[EventService] 重复上报跳过，clientId: {}, serverId: {}",
                        dto.getClientId(), existing.getId());
                return existing;
            }
        }

        return doReportEvent(dto, userId);
    }

    @Override
    public EventInfo findByClientId(String clientId) {
        if (StrUtil.isBlank(clientId)) {
            return null;
        }
        String cacheKey = CLIENT_ID_CACHE_PREFIX + clientId;
        Object cachedId = redisTemplate.opsForValue().get(cacheKey);
        if (cachedId != null) {
            try {
                Long id = Long.valueOf(cachedId.toString());
                EventInfo info = eventInfoMapper.selectById(id);
                if (info != null) {
                    return info;
                }
            } catch (Exception ignore) {
            }
        }

        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventInfo::getClientId, clientId);
        wrapper.last("LIMIT 1");
        EventInfo event = eventInfoMapper.selectOne(wrapper);

        if (event != null) {
            redisTemplate.opsForValue().set(cacheKey, event.getId(), 7, TimeUnit.DAYS);
        }
        return event;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BatchSyncResponseDTO processBatch(List<EventReportDTO> events, Long userId) {
        if (CollUtil.isEmpty(events)) {
            return BatchSyncResponseDTO.builder()
                    .success(true)
                    .message("无数据需要同步")
                    .totalCount(0)
                    .successCount(0)
                    .failedCount(0)
                    .duplicateCount(0)
                    .results(new ArrayList<>())
                    .build();
        }

        List<EventReportDTO> sorted = events.stream()
                .sorted(Comparator.comparingLong(e ->
                        e.getEventTimestamp() != null ? e.getEventTimestamp() : 0L))
                .collect(Collectors.toList());

        List<BatchSyncResponseDTO.SyncResultItem> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int duplicateCount = 0;

        for (EventReportDTO dto : sorted) {
            String clientId = dto.getClientId();
            BatchSyncResponseDTO.SyncResultItem item = BatchSyncResponseDTO.SyncResultItem.builder()
                    .clientId(clientId)
                    .success(false)
                    .duplicate(false)
                    .build();
            try {
                if (StrUtil.isNotBlank(clientId)) {
                    EventInfo existing = findByClientId(clientId);
                    if (existing != null) {
                        item.setDuplicate(true);
                        item.setSuccess(true);
                        item.setServerId(existing.getId());
                        item.setError("重复上报，已跳过");
                        duplicateCount++;
                        results.add(item);
                        continue;
                    }
                }

                EventInfo eventInfo = doReportEvent(dto, userId);
                item.setSuccess(true);
                item.setServerId(eventInfo.getId());
                successCount++;
            } catch (Exception e) {
                log.error("[EventService] 批量同步单条处理失败，clientId: {}", clientId, e);
                item.setError(e.getMessage());
                failedCount++;
            }
            results.add(item);
        }

        boolean allSuccess = failedCount == 0;
        String message = allSuccess
                ? String.format("批量同步完成，成功%d条，重复%d条", successCount, duplicateCount)
                : String.format("批量同步完成，成功%d条，失败%d条，重复%d条", successCount, failedCount, duplicateCount);

        return BatchSyncResponseDTO.builder()
                .success(allSuccess)
                .message(message)
                .totalCount(events.size())
                .successCount(successCount)
                .failedCount(failedCount)
                .duplicateCount(duplicateCount)
                .results(results)
                .build();
    }

    @Override
    public BatchSyncResponseDTO processBatchAsync(List<EventReportDTO> events, Long userId, String deviceId) {
        if (CollUtil.isEmpty(events)) {
            return BatchSyncResponseDTO.builder()
                    .success(true)
                    .message("无数据需要同步")
                    .totalCount(0)
                    .successCount(0)
                    .failedCount(0)
                    .duplicateCount(0)
                    .results(new ArrayList<>())
                    .build();
        }

        String batchId = IdUtil.fastSimpleUUID();
        List<Map<String, Object>> eventMaps = new ArrayList<>();
        for (EventReportDTO dto : events) {
            eventMaps.add(BeanUtil.beanToMap(dto));
        }

        eventSyncProducer.sendBatchSync(eventMaps, userId, deviceId, batchId);

        List<BatchSyncResponseDTO.SyncResultItem> results = events.stream()
                .map(dto -> BatchSyncResponseDTO.SyncResultItem.builder()
                        .clientId(dto.getClientId())
                        .success(true)
                        .duplicate(false)
                        .error("已投递MQ异步处理")
                        .build())
                .collect(Collectors.toList());

        return BatchSyncResponseDTO.builder()
                .success(true)
                .message(String.format("批量同步消息已投递MQ，batchId: %s，数量: %d", batchId, events.size()))
                .totalCount(events.size())
                .successCount(events.size())
                .failedCount(0)
                .duplicateCount(0)
                .results(results)
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    protected EventInfo doReportEvent(EventReportDTO dto, Long userId) {
        EventInfo eventInfo = new EventInfo();
        eventInfo.setClientId(dto.getClientId());
        eventInfo.setEventNo(generateEventNo());
        eventInfo.setTitle(dto.getTitle());
        eventInfo.setEventType(dto.getEventTypeResolved());
        eventInfo.setDescription(dto.getDescription());
        eventInfo.setLng(dto.getLngResolved());
        eventInfo.setLat(dto.getLatResolved());
        eventInfo.setAddress(dto.getAddress());

        List<String> resolvedImages = dto.getImagesResolved();
        if (CollUtil.isNotEmpty(resolvedImages)) {
            eventInfo.setImages(String.join(",", resolvedImages));
        }
        if (CollUtil.isNotEmpty(dto.getVideos())) {
            eventInfo.setVideos(String.join(",", dto.getVideos()));
        }

        Integer anonymous = dto.getAnonymous() != null ? dto.getAnonymous() : 0;
        eventInfo.setAnonymous(anonymous);
        if (anonymous == 1) {
            eventInfo.setReporterName(dto.getReporterName());
            eventInfo.setReporterPhone(dto.getReporterPhone());
        } else if (userId != null) {
            eventInfo.setReporterId(userId);
        } else if (StrUtil.isNotBlank(dto.getReporterId())) {
            try {
                eventInfo.setReporterId(Long.parseLong(dto.getReporterId()));
            } catch (NumberFormatException ignore) {
            }
        }

        eventInfo.setGridId(dto.getGridId());
        eventInfo.setVoiceUrl(dto.getVoiceUrl());
        eventInfo.setStatus(EventStatus.PENDING.getCode());
        eventInfo.setPriority(EventPriority.normalize(dto.getPriority()));

        if (dto.getEventTimestamp() != null) {
            eventInfo.setEventTimestamp(LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(dto.getEventTimestamp()), ZoneId.systemDefault()));
        } else {
            eventInfo.setEventTimestamp(LocalDateTime.now());
        }

        eventInfoMapper.insert(eventInfo);

        if (StrUtil.isNotBlank(eventInfo.getClientId())) {
            redisTemplate.opsForValue().set(
                    CLIENT_ID_CACHE_PREFIX + eventInfo.getClientId(),
                    eventInfo.getId(), 7, TimeUnit.DAYS);
        }

        try {
            String processInstanceId = workflowService.startProcess(eventInfo);
            eventInfo.setProcessInstanceId(processInstanceId);
            eventInfoMapper.updateById(eventInfo);
        } catch (Exception e) {
            log.warn("[EventService] 启动工作流失败，事件ID: {}，错误: {}", eventInfo.getId(), e.getMessage());
        }

        EventProcess process = new EventProcess();
        process.setEventId(eventInfo.getId());
        process.setTaskId("INIT");
        process.setNodeName("上报事件");
        process.setHandlerId(userId);
        process.setAction(ProcessAction.SUBMIT.getCode());
        process.setComment("事件已上报，等待受理");
        process.setHandleTime(LocalDateTime.now());
        eventProcessMapper.insert(process);

        log.info("[EventService] 事件上报成功，事件编号：{}，事件ID：{}，clientId：{}",
                eventInfo.getEventNo(), eventInfo.getId(), eventInfo.getClientId());
        return eventInfo;
    }

    @Override
    public PageResult<EventInfo> getEventList(EventQueryDTO dto) {
        Integer pageNum = dto.getPageNum() != null ? dto.getPageNum() : 1;
        Integer pageSize = dto.getPageSize() != null ? dto.getPageSize() : 10;

        Page<EventInfo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();

        if (StrUtil.isNotBlank(dto.getKeyword())) {
            wrapper.and(w -> w.like(EventInfo::getTitle, dto.getKeyword())
                    .or().like(EventInfo::getEventNo, dto.getKeyword())
                    .or().like(EventInfo::getDescription, dto.getKeyword()));
        }
        if (StrUtil.isNotBlank(dto.getStatus())) {
            wrapper.eq(EventInfo::getStatus, dto.getStatus());
        }
        if (StrUtil.isNotBlank(dto.getEventType())) {
            wrapper.eq(EventInfo::getEventType, dto.getEventType());
        }
        if (dto.getGridId() != null) {
            List<Long> allowed = DataScopeUtils.intersectAccessibleGridIds(Collections.singletonList(dto.getGridId()));
            if (allowed.isEmpty()) {
                wrapper.eq(EventInfo::getId, -1);
            } else {
                wrapper.eq(EventInfo::getGridId, dto.getGridId());
            }
        } else {
            List<Long> accessibleGridIds = DataScopeUtils.getAccessibleGridIds();
            if (CollUtil.isNotEmpty(accessibleGridIds)) {
                wrapper.in(EventInfo::getGridId, accessibleGridIds);
            } else {
                wrapper.eq(EventInfo::getId, -1);
            }
        }
        if (StrUtil.isNotBlank(dto.getStartTime())) {
            wrapper.ge(EventInfo::getCreatedAt, dto.getStartTime());
        }
        if (StrUtil.isNotBlank(dto.getEndTime())) {
            wrapper.le(EventInfo::getCreatedAt, dto.getEndTime());
        }
        if (dto.getReporterId() != null) {
            wrapper.eq(EventInfo::getReporterId, dto.getReporterId());
        }

        wrapper.orderByDesc(EventInfo::getEventTimestamp);
        wrapper.orderByDesc(EventInfo::getCreatedAt);

        Page<EventInfo> result = eventInfoMapper.selectPage(page, wrapper);
        List<EventInfo> records = result.getRecords();
        if (CollUtil.isNotEmpty(records)) {
            List<Long> excludeIds = new ArrayList<>();
            for (EventInfo e : records) {
                try {
                    WarningInfoVO vo = eventUrgeService.getWarningInfo(e);
                    e.setProgressPercent(vo.getProgressPercent() == null ? 0 : vo.getProgressPercent());
                    e.setRemainingHours(vo.getRemainingHours() == null ? 0 : vo.getRemainingHours());
                    e.setRealTimeUrgeLevel(vo.getUrgeLevel() == null ? 0 : vo.getUrgeLevel());
                    if (dto.getUrgeLevel() != null && !dto.getUrgeLevel().equals(e.getRealTimeUrgeLevel())) {
                        excludeIds.add(e.getId());
                    }
                } catch (Exception ex) {
                    log.warn("计算预警信息失败，事件ID：{}", e.getId(), ex);
                }
            }
            if (dto.getUrgeLevel() != null && !excludeIds.isEmpty()) {
                records.removeIf(e -> excludeIds.contains(e.getId()));
            }
        }
        return PageResult.of(result.getTotal(), records, pageNum, pageSize);
    }

    @Override
    public EventDetailVO getEventDetail(Long eventId) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        if (!DataScopeUtils.canAccessGrid(eventInfo.getGridId())) {
            throw new BusinessException("无权访问该事件");
        }

        LambdaQueryWrapper<EventProcess> processWrapper = new LambdaQueryWrapper<>();
        processWrapper.eq(EventProcess::getEventId, eventId);
        processWrapper.orderByAsc(EventProcess::getCreatedAt);
        List<EventProcess> processList = eventProcessMapper.selectList(processWrapper);

        LambdaQueryWrapper<EventEvaluation> evalWrapper = new LambdaQueryWrapper<>();
        evalWrapper.eq(EventEvaluation::getEventId, eventId);
        EventEvaluation evaluation = eventEvaluationMapper.selectOne(evalWrapper);

        EventDetailVO vo = new EventDetailVO();
        vo.setEventInfo(eventInfo);
        vo.setProcessList(processList);
        vo.setEvaluation(evaluation);
        vo.setComparisonList(imageComparisonService.getComparisonHistory(eventId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEventStatus(Long eventId, String status) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }
        eventInfo.setStatus(status);
        eventInfoMapper.updateById(eventInfo);
        log.info("[EventService] 事件状态更新，事件ID：{}，新状态：{}", eventId, status);
    }

    @Override
    public String generateEventNo() {
        String dateStr = DateUtil.format(DateUtil.date(), "yyyyMMdd");
        String key = EVENT_NO_KEY + dateStr;

        Long seq = redisTemplate.opsForValue().increment(key, 1);
        if (seq != null && seq == 1) {
            redisTemplate.expire(key, 2, TimeUnit.DAYS);
        }

        if (seq == null) {
            throw new BusinessException("生成事件编号失败");
        }

        return "EVT" + dateStr + String.format("%06d", seq);
    }
}
