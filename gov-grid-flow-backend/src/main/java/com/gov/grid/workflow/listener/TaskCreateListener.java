package com.gov.grid.workflow.listener;

import com.gov.grid.entity.SysNotification;
import com.gov.grid.mapper.SysNotificationMapper;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TaskCreateListener implements TaskListener {

    private static final Logger log = LoggerFactory.getLogger(TaskCreateListener.class);

    private static ApplicationContext applicationContext;

    @Autowired
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            String taskId = delegateTask.getId();
            String taskName = delegateTask.getName();
            String processInstanceId = delegateTask.getProcessInstanceId();
            Set<String> candidateGroups = delegateTask.getCandidates() != null
                    ? delegateTask.getCandidates()
                    : java.util.Collections.emptySet();
            String eventId = delegateTask.getVariable("eventId") != null
                    ? delegateTask.getVariable("eventId").toString()
                    : "";
            log.info("任务创建通知 - 任务ID: {}, 任务名称: {}, 流程实例ID: {}, 候选组: {}, 事件ID: {}",
                    taskId, taskName, processInstanceId, candidateGroups, eventId);
            SysNotificationMapper notificationMapper = applicationContext.getBean(SysNotificationMapper.class);
            if (notificationMapper == null) {
                log.warn("SysNotificationMapper 未找到，跳过通知发送");
                return;
            }
            for (String group : candidateGroups) {
                SysNotification notification = new SysNotification();
                notification.setTitle("待办任务通知");
                notification.setContent("您有新的待办任务: " + taskName + "，请及时处理。");
                notification.setType("TASK");
                if (!eventId.isEmpty()) {
                    try {
                        notification.setBizId(Long.parseLong(eventId));
                    } catch (NumberFormatException e) {
                        log.debug("eventId 不是 Long 类型: {}", eventId);
                    }
                }
                notification.setIsRead(0);
                notificationMapper.insert(notification);
                log.info("已向候选组 [{}] 发送任务通知，任务名称: {}", group, taskName);
            }
            TaskService taskService = applicationContext.getBean(TaskService.class);
            if (taskService != null) {
                taskService.addComment(taskId, processInstanceId, "NOTIFY",
                        "任务创建通知已发送给候选组: " + candidateGroups);
            }
        } catch (Exception e) {
            log.error("任务创建监听器处理异常", e);
        }
    }
}
