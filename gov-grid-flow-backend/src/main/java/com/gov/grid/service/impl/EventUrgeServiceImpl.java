package com.gov.grid.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.EventUrgeRecord;
import com.gov.grid.entity.EventUrgeRule;
import com.gov.grid.entity.EventUrgeTemplate;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysUser;
import com.gov.grid.enums.EventPriority;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.UrgeLevelEnum;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.EventUrgeRecordMapper;
import com.gov.grid.mapper.EventUrgeRuleMapper;
import com.gov.grid.mapper.EventUrgeTemplateMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.EventUrgeService;
import com.gov.grid.service.NotificationService;
import com.gov.grid.vo.WarningInfoVO;
import com.gov.grid.workflow.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventUrgeServiceImpl implements EventUrgeService {

    private static final int DEFAULT_TIME_LIMIT_HOURS = 24;
    private static final double DEFAULT_WARNING_RATIO = 0.2;
    private static final long DEFAULT_RECEIVER_ID = 1L;

    private final EventUrgeRuleMapper eventUrgeRuleMapper;
    private final EventUrgeTemplateMapper eventUrgeTemplateMapper;
    private final EventUrgeRecordMapper eventUrgeRecordMapper;
    private final EventInfoMapper eventInfoMapper;
    private final GridInfoMapper gridInfoMapper;
    private final NotificationService notificationService;
    private final WorkflowService workflowService;
    private final SysUserMapper sysUserMapper;

    @Override
    public List<EventUrgeRule> listRules() {
        LambdaQueryWrapper<EventUrgeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EventUrgeRule::getCreatedAt);
        return eventUrgeRuleMapper.selectList(wrapper);
    }

    @Override
    public EventUrgeRule getRuleById(Long id) {
        return eventUrgeRuleMapper.selectById(id);
    }

    @Override
    public EventUrgeRule getRuleByEventType(String eventType) {
        LambdaQueryWrapper<EventUrgeRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventUrgeRule::getEventType, eventType);
        wrapper.eq(EventUrgeRule::getEnabled, 1);
        wrapper.last("LIMIT 1");
        return eventUrgeRuleMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventUrgeRule saveRule(EventUrgeRule rule) {
        eventUrgeRuleMapper.insert(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventUrgeRule updateRule(EventUrgeRule rule) {
        eventUrgeRuleMapper.updateById(rule);
        return rule;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRule(Long id) {
        return eventUrgeRuleMapper.deleteById(id) > 0;
    }

    @Override
    public List<EventUrgeTemplate> listTemplates() {
        LambdaQueryWrapper<EventUrgeTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EventUrgeTemplate::getCreatedAt);
        return eventUrgeTemplateMapper.selectList(wrapper);
    }

    @Override
    public EventUrgeTemplate getTemplateById(Long id) {
        return eventUrgeTemplateMapper.selectById(id);
    }

    @Override
    public EventUrgeTemplate getTemplateByCode(String templateCode) {
        LambdaQueryWrapper<EventUrgeTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventUrgeTemplate::getTemplateCode, templateCode);
        wrapper.eq(EventUrgeTemplate::getEnabled, 1);
        wrapper.last("LIMIT 1");
        return eventUrgeTemplateMapper.selectOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventUrgeTemplate saveTemplate(EventUrgeTemplate template) {
        eventUrgeTemplateMapper.insert(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventUrgeTemplate updateTemplate(EventUrgeTemplate template) {
        eventUrgeTemplateMapper.updateById(template);
        return template;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTemplate(Long id) {
        return eventUrgeTemplateMapper.deleteById(id) > 0;
    }

    @Override
    public PageResult<EventUrgeRecord> listRecords(Long eventId, Integer page, Integer size) {
        Integer pageNum = page != null ? page : 1;
        Integer pageSize = size != null ? size : 10;

        Page<EventUrgeRecord> pageObj = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<EventUrgeRecord> wrapper = new LambdaQueryWrapper<>();

        if (eventId != null) {
            wrapper.eq(EventUrgeRecord::getEventId, eventId);
        }

        wrapper.orderByDesc(EventUrgeRecord::getCreatedAt);

        Page<EventUrgeRecord> result = eventUrgeRecordMapper.selectPage(pageObj, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), pageNum, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventUrgeRecord createRecord(EventUrgeRecord record) {
        eventUrgeRecordMapper.insert(record);
        return record;
    }

    @Override
    public LocalDateTime calculateDeadline(String eventType, LocalDateTime startTime) {
        EventUrgeRule rule = getRuleByEventType(eventType);
        int timeLimitHours = rule != null && rule.getTimeLimitHours() != null
                ? rule.getTimeLimitHours()
                : DEFAULT_TIME_LIMIT_HOURS;
        return startTime.plusHours(timeLimitHours);
    }

    @Override
    public WarningInfoVO getWarningInfo(EventInfo event) {
        if (event == null) {
            return null;
        }

        EventUrgeRule rule = getRuleByEventType(event.getEventType());
        int timeLimitHours = rule != null && rule.getTimeLimitHours() != null
                ? rule.getTimeLimitHours()
                : DEFAULT_TIME_LIMIT_HOURS;
        double warningRatio = rule != null && rule.getWarningRatio() != null
                ? rule.getWarningRatio().doubleValue()
                : DEFAULT_WARNING_RATIO;

        LocalDateTime startTime = event.getDispatchedAt() != null
                ? event.getDispatchedAt()
                : event.getCreatedAt();

        LocalDateTime deadline = calculateDeadline(event.getEventType(), startTime);

        double remaining = Duration.between(LocalDateTime.now(), deadline).toMillis() / 3600000.0;
        double total = timeLimitHours;
        double progress = (1 - remaining / total) * 100;

        boolean isOverdue = remaining <= 0;
        boolean isWarning = !isOverdue && (remaining / total) < warningRatio;

        int urgeLevel;
        if (isOverdue) {
            urgeLevel = 2;
        } else if (isWarning) {
            urgeLevel = 1;
        } else {
            urgeLevel = 0;
        }

        if (event.getUrgeLevel() != null && event.getUrgeLevel() >= 3) {
            urgeLevel = 3;
        }

        WarningInfoVO vo = new WarningInfoVO();
        vo.setUrgeLevel(urgeLevel);
        vo.setUrgeLevelDesc(UrgeLevelEnum.getDescByCode(urgeLevel));
        vo.setRemainingHours(remaining);
        vo.setDeadlineAt(deadline);
        vo.setTimeLimitHours(timeLimitHours);
        vo.setIsWarning(isWarning);
        vo.setIsOverdue(isOverdue);
        vo.setProgressPercent(progress);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scanAndUrge() {
        List<String> statusList = Arrays.asList(
                EventStatus.APPROVED.getCode(),
                EventStatus.DISPATCHED.getCode(),
                EventStatus.HANDLED.getCode()
        );

        LambdaQueryWrapper<EventInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(EventInfo::getStatus, statusList);
        wrapper.eq(EventInfo::getDeleted, 0);
        List<EventInfo> eventList = eventInfoMapper.selectList(wrapper);

        int count = 0;

        for (EventInfo event : eventList) {
            try {
                WarningInfoVO vo = getWarningInfo(event);
                if (vo == null) {
                    continue;
                }

                int currentDbLevel = event.getUrgeLevel() == null ? 0 : event.getUrgeLevel();
                int newLevel;

                if (vo.getIsOverdue()) {
                    newLevel = 2;
                } else if (vo.getIsWarning()) {
                    newLevel = 1;
                } else {
                    newLevel = 0;
                }

                LocalDateTime startTime = event.getDispatchedAt() != null
                        ? event.getDispatchedAt()
                        : event.getCreatedAt();
                long hoursSinceStart = Duration.between(startTime, LocalDateTime.now()).toHours();
                if (vo.getIsOverdue() && hoursSinceStart > 24) {
                    newLevel = 3;
                }

                if (newLevel > currentDbLevel) {
                    event.setUrgeLevel(newLevel);
                    event.setDeadlineAt(vo.getDeadlineAt());
                    eventInfoMapper.updateById(event);

                    EventUrgeTemplate template = findTemplateByUrgeLevel(newLevel);
                    String title = template != null ? renderTemplate(template.getTitleTemplate(), event, vo) : "事件催办通知";
                    String content = template != null ? renderTemplate(template.getContentTemplate(), event, vo) : "请及时处理事件";
                    String channel = template != null ? template.getChannel() : "APP";

                    Long receiverId = getReceiverId(event);
                    SysUser receiver = sysUserMapper.selectById(receiverId);
                    String receiverName = receiver != null ? receiver.getRealName() : "处置员";
                    String receiverPhone = receiver != null ? receiver.getPhone() : null;
                    String receiverEmail = receiver != null ? receiver.getEmail() : null;
                    String notifyType = resolveNotifyType(newLevel);

                    boolean sendOk = notificationService.sendByChannel(
                            channel, receiverId, receiverName, receiverPhone, receiverEmail,
                            title, content, notifyType, event.getId());

                    EventUrgeRecord record = new EventUrgeRecord();
                    record.setEventId(event.getId());
                    record.setEventNo(event.getEventNo());
                    record.setUrgeLevel(newLevel);
                    record.setTemplateId(template != null ? template.getId() : null);
                    record.setTitle(title);
                    record.setContent(content);
                    record.setChannel(channel);
                    record.setReceiverId(receiverId);
                    record.setReceiverName(receiverName);
                    record.setSendStatus(sendOk ? 1 : 2);
                    record.setErrorMsg(sendOk ? null : "渠道" + channel + "发送失败");
                    eventUrgeRecordMapper.insert(record);

                    count++;
                    log.info("[EventUrgeService] 事件催办，事件ID：{}，催办级别：{}，渠道：{}，发送：{}",
                            event.getId(), newLevel, channel, sendOk ? "成功" : "失败");
                }
            } catch (Exception e) {
                log.error("[EventUrgeService] 扫描催办异常，事件ID：{}", event.getId(), e);
            }
        }

        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean escalateEvent(EventInfo event, String escalateLevel) {
        event.setUrgeLevel(3);
        event.setPriority(EventPriority.URGENT.getCode());
        eventInfoMapper.updateById(event);

        Long superiorReceiverId = getSuperiorReceiverId(event);
        boolean reassignOk = false;
        try {
            if (StrUtil.isNotBlank(event.getProcessInstanceId())) {
                List<Task> tasks = workflowService.getTaskList(null, event.getProcessInstanceId());
                if (CollUtil.isNotEmpty(tasks)) {
                    String taskId = tasks.get(0).getId();
                    String newAssigneeId = String.valueOf(superiorReceiverId);
                    try {
                        workflowService.assignTask(taskId, newAssigneeId);
                        reassignOk = true;
                        log.info("[EventUrgeService] 督办升级：流程任务改派成功，事件ID：{}，任务ID：{}，新assignee：{}",
                                event.getId(), taskId, newAssigneeId);
                    } catch (Exception assignEx) {
                        log.warn("[EventUrgeService] 督办升级：流程任务改派失败，事件ID：{}，错误：{}",
                                event.getId(), assignEx.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("[EventUrgeService] 督办升级：改派流程异常，事件ID：{}", event.getId(), e);
        }

        WarningInfoVO vo = getWarningInfo(event);
        EventUrgeTemplate template = findTemplateByUrgeLevel(3);
        String title = template != null ? renderTemplate(template.getTitleTemplate(), event, vo) : "升级督办通知";
        String content = template != null ? renderTemplate(template.getContentTemplate(), event, vo)
                : "事件已升级督办，请上级关注处理" + (reassignOk ? "，流程任务已改派" : "");
        String channel = template != null ? template.getChannel() : "ALL";

        SysUser receiver = sysUserMapper.selectById(superiorReceiverId);
        String receiverName = receiver != null ? receiver.getRealName() : "上级负责人";
        String receiverPhone = receiver != null ? receiver.getPhone() : null;
        String receiverEmail = receiver != null ? receiver.getEmail() : null;

        boolean sendOk = notificationService.sendByChannel(
                channel, superiorReceiverId, receiverName, receiverPhone, receiverEmail,
                title, content, "URGE_ESCALATED", event.getId());

        EventUrgeRecord record = new EventUrgeRecord();
        record.setEventId(event.getId());
        record.setEventNo(event.getEventNo());
        record.setUrgeLevel(3);
        record.setTemplateId(template != null ? template.getId() : null);
        record.setTitle(title);
        record.setContent(content);
        record.setChannel(channel);
        record.setReceiverId(superiorReceiverId);
        record.setReceiverName(receiverName);
        record.setSendStatus(sendOk ? 1 : 2);
        record.setErrorMsg(sendOk ? null : "督办升级通知发送失败");
        eventUrgeRecordMapper.insert(record);

        log.info("[EventUrgeService] 事件升级督办，事件ID：{}，接收人：{}，渠道：{}，改派：{}，通知：{}",
                event.getId(), receiverName, channel, reassignOk ? "成功" : "未执行", sendOk ? "成功" : "失败");
        return true;
    }

    private EventUrgeTemplate findTemplateByUrgeLevel(int urgeLevel) {
        String templateCode;
        switch (urgeLevel) {
            case 1:
                templateCode = "URGE_WARNING";
                break;
            case 2:
                templateCode = "URGE_OVERDUE";
                break;
            case 3:
                templateCode = "URGE_ESCALATED";
                break;
            default:
                return null;
        }
        return getTemplateByCode(templateCode);
    }

    private String renderTemplate(String template, EventInfo event, WarningInfoVO vo) {
        if (StrUtil.isBlank(template)) {
            return "";
        }
        String result = template;
        result = StrUtil.replace(result, "${eventNo}", event.getEventNo() != null ? event.getEventNo() : "");
        result = StrUtil.replace(result, "${title}", event.getTitle() != null ? event.getTitle() : "");
        if (vo != null && vo.getDeadlineAt() != null) {
            result = StrUtil.replace(result, "${deadline}",
                    vo.getDeadlineAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        } else {
            result = StrUtil.replace(result, "${deadline}", "");
        }
        if (vo != null && vo.getRemainingHours() != null) {
            result = StrUtil.replace(result, "${remainingHours}",
                    String.format("%.1f", Math.abs(vo.getRemainingHours())));
        } else {
            result = StrUtil.replace(result, "${remainingHours}", "");
        }
        return result;
    }

    private Long getReceiverId(EventInfo event) {
        if (event.getGridId() != null) {
            GridInfo gridInfo = gridInfoMapper.selectById(event.getGridId());
            if (gridInfo != null && gridInfo.getGridLeaderId() != null) {
                return gridInfo.getGridLeaderId();
            }
        }
        return DEFAULT_RECEIVER_ID;
    }

    private Long getSuperiorReceiverId(EventInfo event) {
        if (event.getGridId() != null) {
            GridInfo gridInfo = gridInfoMapper.selectById(event.getGridId());
            if (gridInfo != null && gridInfo.getParentId() != null) {
                GridInfo parentGrid = gridInfoMapper.selectById(gridInfo.getParentId());
                if (parentGrid != null && parentGrid.getGridLeaderId() != null) {
                    return parentGrid.getGridLeaderId();
                }
            }
            if (gridInfo != null && gridInfo.getGridLeaderId() != null) {
                return gridInfo.getGridLeaderId();
            }
        }
        return DEFAULT_RECEIVER_ID;
    }

    private String resolveNotifyType(int urgeLevel) {
        switch (urgeLevel) {
            case 1:
                return "URGE_WARNING";
            case 2:
                return "URGE_OVERDUE";
            case 3:
                return "URGE_ESCALATED";
            default:
                return "URGE";
        }
    }
}
