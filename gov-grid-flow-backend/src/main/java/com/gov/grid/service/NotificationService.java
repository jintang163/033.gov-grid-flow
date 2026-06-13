package com.gov.grid.service;

import java.util.Map;

public interface NotificationService {

    boolean sendSms(String phone, String templateCode, Map<String, Object> params);

    boolean sendEmail(String to, String subject, String content);

    boolean sendAppPush(Long userId, String title, String content, String type, Long bizId);

    boolean sendByChannel(String channel, Long receiverId, String receiverName, String receiverPhone,
                          String receiverEmail, String title, String content, String type, Long bizId);
}
