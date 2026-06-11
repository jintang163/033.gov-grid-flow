package com.gov.grid.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.EventProcessDTO;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventProcess;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.EventProcessMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.EventProcessService;
import com.gov.grid.service.EventService;
import com.gov.grid.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventProcessServiceImpl implements EventProcessService {

    private final WorkflowService workflowService;
    private final EventService eventService;
    private final EventInfoMapper eventInfoMapper;
    private final EventProcessMapper eventProcessMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processEvent(EventProcessDTO dto, Long userId) {
        EventInfo eventInfo = eventInfoMapper.selectById(dto.getEventId());
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        SysUser user = sysUserMapper.selectById(userId);
        String handlerName = user != null ? user.getRealName() : "未知用户";

        String taskId = dto.getTaskId() != null ? String.valueOf(dto.getTaskId()) : null;
        if (StrUtil.isBlank(taskId)) {
            List<Task> tasks = workflowService.getTaskList(String.valueOf(userId), eventInfo.getProcessInstanceId());
            if (CollUtil.isEmpty(tasks)) {
                throw new BusinessException("未找到待处理任务");
            }
            taskId = tasks.get(0).getId();
        }

        String action = dto.getAction();
        String nodeName = resolveNodeName(action);

        EventProcess process = new EventProcess();
        process.setEventId(dto.getEventId());
        process.setTaskId(taskId);
        process.setNodeName(nodeName);
        process.setHandlerId(userId);
        process.setHandlerName(handlerName);
        process.setAction(action);
        process.setComment(dto.getComment());
        if (CollUtil.isNotEmpty(dto.getAttachments())) {
            process.setAttachments(String.join(",", dto.getAttachments()));
        }
        process.setHandleTime(LocalDateTime.now());

        LambdaQueryWrapper<EventProcess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventProcess::getEventId, dto.getEventId())
                .orderByDesc(EventProcess::getHandleTime)
                .last("LIMIT 1");
        EventProcess lastProcess = eventProcessMapper.selectOne(wrapper);
        if (lastProcess != null && lastProcess.getHandleTime() != null) {
            long seconds = ChronoUnit.SECONDS.between(lastProcess.getHandleTime(), process.getHandleTime());
            process.setDurationSeconds(seconds);
        }

        eventProcessMapper.insert(process);

        Map<String, Object> variables = new HashMap<>();
        if ("REJECT".equals(action)) {
            workflowService.rejectTask(taskId, String.valueOf(userId), dto.getComment());
            eventService.updateEventStatus(dto.getEventId(), "REJECTED");
        } else {
            if ("APPROVE".equals(action)) {
                variables.put("approved", true);
                eventService.updateEventStatus(dto.getEventId(), "APPROVED");
            } else if ("DISPATCH".equals(action)) {
                variables.put("dispatched", true);
                eventService.updateEventStatus(dto.getEventId(), "DISPATCHED");
            } else if ("HANDLE".equals(action)) {
                variables.put("handleCompleted", true);
                eventService.updateEventStatus(dto.getEventId(), "HANDLED");
            } else if ("VERIFY".equals(action)) {
                variables.put("verifyPassed", true);
                eventService.updateEventStatus(dto.getEventId(), "COMPLETED");
            }
            workflowService.completeTask(taskId, String.valueOf(userId), variables);
        }

        log.info("事件处理完成，事件ID：{}，操作：{}，处理人：{}", dto.getEventId(), action, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTask(Long eventId, String taskId, String assigneeId, Long userId) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        SysUser assignee = sysUserMapper.selectById(Long.parseLong(assigneeId));
        if (assignee == null) {
            throw new BusinessException("被分派用户不存在");
        }

        SysUser user = sysUserMapper.selectById(userId);
        String handlerName = user != null ? user.getRealName() : "未知用户";

        if (taskId == null || taskId.isEmpty()) {
            List<Task> tasks = workflowService.getTaskList(null, eventInfo.getProcessInstanceId());
            if (CollUtil.isEmpty(tasks)) {
                throw new BusinessException("未找到可分派的任务");
            }
            taskId = tasks.get(0).getId();
        }

        workflowService.assignTask(taskId, assigneeId);

        EventProcess process = new EventProcess();
        process.setEventId(eventId);
        process.setTaskId(taskId);
        process.setNodeName("分派任务");
        process.setHandlerId(userId);
        process.setHandlerName(handlerName);
        process.setAction("ASSIGN");
        process.setComment("分派给：" + assignee.getRealName() + "(" + assigneeId + ")");
        process.setHandleTime(LocalDateTime.now());
        eventProcessMapper.insert(process);

        eventInfo.setStatus("DISPATCHED");
        eventInfoMapper.updateById(eventInfo);

        log.info("任务分派成功，事件ID：{}，任务ID：{}，分派给：{}，操作人：{}", eventId, taskId, assigneeId, userId);
    }

    @Override
    public List<EventProcess> getProcessHistory(Long eventId) {
        LambdaQueryWrapper<EventProcess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventProcess::getEventId, eventId);
        wrapper.orderByAsc(EventProcess::getHandleTime);
        return eventProcessMapper.selectList(wrapper);
    }

    @Override
    public String getProcessDiagram(Long eventId) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }
        if (StrUtil.isBlank(eventInfo.getProcessInstanceId())) {
            throw new BusinessException("流程实例不存在");
        }

        try (InputStream is = workflowService.getProcessDiagram(eventInfo.getProcessInstanceId())) {
            byte[] bytes = IoUtil.readBytes(is);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            log.error("获取流程图失败，事件ID：{}", eventId, e);
            throw new BusinessException("获取流程图失败：" + e.getMessage());
        }
    }

    private String resolveNodeName(String action) {
        switch (action) {
            case "APPROVE":
                return "审核受理";
            case "DISPATCH":
                return "任务分派";
            case "HANDLE":
                return "事件处置";
            case "VERIFY":
                return "核查结案";
            case "REJECT":
                return "退回";
            default:
                return "处理";
        }
    }
}
