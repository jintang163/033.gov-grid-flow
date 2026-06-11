package com.gov.grid.workflow.listener;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.SysNotificationMapper;
import com.gov.grid.mapper.SysUserMapper;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.identity.api.IdentityLink;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
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
            Set<IdentityLink> candidates = delegateTask.getCandidates();
            String eventId = delegateTask.getVariable("eventId") != null
                    ? delegateTask.getVariable("eventId").toString()
                    : "";
            log.info("任务创建通知 - 任务ID: {}, 任务名称: {}, 流程实例ID: {}, 候选身份: {}, 事件ID: {}",
                    taskId, taskName, processInstanceId, candidates, eventId);

            SysNotificationMapper notificationMapper = applicationContext.getBean(SysNotificationMapper.class);
            if (notificationMapper == null) {
                log.warn("SysNotificationMapper 未找到，跳过通知发送");
                return;
            }
            SysUserMapper sysUserMapper = applicationContext.getBean(SysUserMapper.class);
            if (sysUserMapper == null) {
                log.warn("SysUserMapper 未找到，跳过通知发送");
                return;
            }

            if (candidates == null || candidates.isEmpty()) {
                log.info("无候选组/用户，跳过通知发送");
                return;
            }

            for (IdentityLink link : candidates) {
                String groupId = link.getGroupId();
                String userId = link.getUserId();

                if (StrUtil.isNotBlank(groupId)) {
                    LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
                    userWrapper.eq(SysUser::getRole, groupId).eq(SysUser::getStatus, 1);
                    List<SysUser> users = sysUserMapper.selectList(userWrapper);
                    for (SysUser user : users) {
                        createNotification(notificationMapper, user.getId(), taskName, eventId);
                    }
                    log.info("已向候选组 [{}] 的 {} 名用户发送任务通知，任务名称: {}", groupId, users.size(), taskName);
                } else if (StrUtil.isNotBlank(userId)) {
                    SysUser user = sysUserMapper.selectById(Long.parseLong(userId));
                    if (user != null) {
                        createNotification(notificationMapper, user.getId(), taskName, eventId);
                        log.info("已向候选用户 [{}] 发送任务通知，任务名称: {}", userId, taskName);
                    } else {
                        log.warn("候选用户 [{}] 未找到，跳过通知发送", userId);
                    }
                }
            }

            TaskService taskService = applicationContext.getBean(TaskService.class);
            if (taskService != null) {
                taskService.addComment(taskId, processInstanceId, "NOTIFY",
                        "任务创建通知已发送给候选身份");
            }
        } catch (Exception e) {
            log.error("任务创建监听器处理异常", e);
        }
    }

    private void createNotification(SysNotificationMapper notificationMapper, Long userId, String taskName, String eventId) {
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle("待办任务通知");
        notification.setContent("您有新的待办任务: " + taskName + "，请及时处理。");
        notification.setType("TASK");
        if (StrUtil.isNotBlank(eventId)) {
            try {
                notification.setBizId(Long.parseLong(eventId));
            } catch (NumberFormatException e) {
                log.debug("eventId 不是 Long 类型: {}", eventId);
            }
        }
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }
}
