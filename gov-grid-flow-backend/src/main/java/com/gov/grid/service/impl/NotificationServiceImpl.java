package com.gov.grid.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.entity.SysUser;
import com.gov.grid.mapper.SysNotificationMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String NOTIFICATION_TOPIC = "GOV_GRID_NOTIFICATION";

    private final RocketMQTemplate rocketMQTemplate;
    private final SysNotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public void sendNotification(Long userId, String title, String content, String type, String bizId) {
        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        message.put("title", title);
        message.put("content", content);
        message.put("type", type);
        message.put("bizId", bizId);
        message.put("action", "SAVE_NOTIFICATION");

        rocketMQTemplate.convertAndSend(NOTIFICATION_TOPIC, JSONUtil.toJsonStr(message));
        log.info("发送站内通知消息，userId：{}，title：{}", userId, title);
    }

    @Override
    public void sendSms(String phone, String content) {
        log.info("【模拟发送短信】手机号：{}，内容：{}", phone, content);

        Map<String, Object> message = new HashMap<>();
        message.put("phone", phone);
        message.put("content", content);
        message.put("action", "SEND_SMS");

        rocketMQTemplate.convertAndSend(NOTIFICATION_TOPIC, JSONUtil.toJsonStr(message));
    }

    @Override
    public void sendDingTalk(String userId, String content) {
        log.info("【模拟发送钉钉】用户ID：{}，内容：{}", userId, content);

        Map<String, Object> message = new HashMap<>();
        message.put("userId", userId);
        message.put("content", content);
        message.put("action", "SEND_DINGTALK");

        rocketMQTemplate.convertAndSend(NOTIFICATION_TOPIC, JSONUtil.toJsonStr(message));
    }

    @Override
    public void markAsRead(Long id, Long userId) {
        SysNotification notification = notificationMapper.selectById(id);
        if (notification == null) {
            throw new BusinessException("通知不存在");
        }
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该通知");
        }

        LambdaUpdateWrapper<SysNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotification::getId, id)
                .set(SysNotification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
        log.info("通知标记已读，通知ID：{}，用户ID：{}", id, userId);
    }

    @Override
    public void markAllAsRead(Long userId) {
        LambdaUpdateWrapper<SysNotification> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0)
                .set(SysNotification::getIsRead, 1);
        notificationMapper.update(null, wrapper);
        log.info("用户全部通知标记已读，用户ID：{}", userId);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getUserId, userId)
                .eq(SysNotification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper);
    }

    @Override
    public PageResult<SysNotification> getNotificationList(Long userId, Integer pageNum, Integer pageSize) {
        Integer currentPage = pageNum != null ? pageNum : 1;
        Integer currentSize = pageSize != null ? pageSize : 10;

        Page<SysNotification> page = new Page<>(currentPage, currentSize);
        LambdaQueryWrapper<SysNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotification::getUserId, userId)
                .orderByDesc(SysNotification::getCreatedAt);

        Page<SysNotification> result = notificationMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords(), currentPage, currentSize);
    }

    @Override
    public void callMember(Long userId) {
        if (userId == null) {
            throw new BusinessException("用户ID不能为空");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("网格员不存在");
        }

        String userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
        String title = "调度呼叫通知";
        String content = "指挥中心向您发起调度呼叫，请尽快前往事件现场处置！";

        sendNotification(userId, title, content, "DISPATCH_CALL", String.valueOf(userId));

        if (user.getPhone() != null && !user.getPhone().isEmpty()) {
            String smsContent = "【网格化管理】" + userName + "您好，指挥中心向您发起调度呼叫，请尽快处理！";
            sendSms(user.getPhone(), smsContent);
        }

        log.info("一键呼叫网格员完成，userId：{}，userName：{}，phone：{}", userId, userName, user.getPhone());
    }
}
