package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.entity.SysNotification;

public interface NotificationService {

    void sendNotification(Long userId, String title, String content, String type, String bizId);

    void sendSms(String phone, String content);

    void sendDingTalk(String userId, String content);

    void markAsRead(Long id, Long userId);

    void markAllAsRead(Long userId);

    Long getUnreadCount(Long userId);

    PageResult<SysNotification> getNotificationList(Long userId, Integer pageNum, Integer pageSize);
}
