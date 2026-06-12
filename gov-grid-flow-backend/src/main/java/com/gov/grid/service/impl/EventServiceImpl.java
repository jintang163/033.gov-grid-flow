package com.gov.grid.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.EventQueryDTO;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventProcess;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.EventPriority;
import com.gov.grid.enums.ProcessAction;
import com.gov.grid.mapper.EventEvaluationMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.EventProcessMapper;
import com.gov.grid.security.DataScopeUtils;
import com.gov.grid.service.EventService;
import com.gov.grid.workflow.WorkflowService;
import com.gov.grid.vo.EventDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String EVENT_NO_KEY = "event:no:";
    private static final String EVENT_NO_LOCK_KEY = "event:no:lock";
    private static final long EVENT_NO_LOCK_EXPIRE = 10;

    private final EventInfoMapper eventInfoMapper;
    private final EventProcessMapper eventProcessMapper;
    private final EventEvaluationMapper eventEvaluationMapper;
    private final WorkflowService workflowService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventInfo reportEvent(EventReportDTO dto, Long userId) {
        EventInfo eventInfo = new EventInfo();
        eventInfo.setEventNo(generateEventNo());
        eventInfo.setTitle(dto.getTitle());
        eventInfo.setEventType(dto.getEventType());
        eventInfo.setDescription(dto.getDescription());
        eventInfo.setLng(dto.getLng());
        eventInfo.setLat(dto.getLat());
        eventInfo.setAddress(dto.getAddress());

        if (CollUtil.isNotEmpty(dto.getImages())) {
            eventInfo.setImages(String.join(",", dto.getImages()));
        }
        if (CollUtil.isNotEmpty(dto.getVideos())) {
            eventInfo.setVideos(String.join(",", dto.getVideos()));
        }

        eventInfo.setAnonymous(dto.getAnonymous());
        if (dto.getAnonymous() == 1) {
            eventInfo.setReporterName(dto.getReporterName());
            eventInfo.setReporterPhone(dto.getReporterPhone());
        } else if (userId != null) {
            eventInfo.setReporterId(userId);
        }

        eventInfo.setGridId(dto.getGridId());
        eventInfo.setStatus(EventStatus.PENDING.getCode());
        eventInfo.setPriority(dto.getPriority() != null ? dto.getPriority() : EventPriority.NORMAL.getCode());

        eventInfoMapper.insert(eventInfo);

        String processInstanceId = workflowService.startProcess(eventInfo);
        eventInfo.setProcessInstanceId(processInstanceId);
        eventInfoMapper.updateById(eventInfo);

        EventProcess process = new EventProcess();
        process.setEventId(eventInfo.getId());
        process.setTaskId("INIT");
        process.setNodeName("上报事件");
        process.setHandlerId(userId);
        process.setAction(ProcessAction.SUBMIT.getCode());
        process.setComment("事件已上报，等待受理");
        process.setHandleTime(LocalDateTime.now());
        eventProcessMapper.insert(process);

        log.info("事件上报成功，事件编号：{}，事件ID：{}", eventInfo.getEventNo(), eventInfo.getId());
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

        wrapper.orderByDesc(EventInfo::getCreatedAt);

        Page<EventInfo> result = eventInfoMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), pageNum, pageSize);
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
        log.info("事件状态更新，事件ID：{}，新状态：{}", eventId, status);
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
