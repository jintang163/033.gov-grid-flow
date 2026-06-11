package com.gov.grid.mq;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = "GOV_GRID_NOTIFICATION", consumerGroup = "gov-grid-notification-consumer")
public class NotificationConsumer implements RocketMQListener<String> {

    private final SysNotificationMapper notificationMapper;

    @Override
    public void onMessage(String message) {
        try {
            JSONObject json = JSONUtil.parseObj(message);
            String action = json.getStr("action");

            switch (action) {
                case "SAVE_NOTIFICATION":
                    handleSaveNotification(json);
                    break;
                case "SEND_SMS":
                    handleSendSms(json);
                    break;
                case "SEND_DINGTALK":
                    handleSendDingTalk(json);
                    break;
                default:
                    log.warn("未知的消息动作类型：{}", action);
            }
        } catch (Exception e) {
            log.error("处理通知消息失败，消息内容：{}", message, e);
        }
    }

    private void handleSaveNotification(JSONObject json) {
        Long userId = json.getLong("userId");
        String title = json.getStr("title");
        String content = json.getStr("content");
        String type = json.getStr("type");
        Long bizId = json.getLong("bizId");

        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setBizId(bizId);
        notification.setIsRead(0);

        notificationMapper.insert(notification);
        log.info("站内通知保存成功，userId：{}，title：{}", userId, title);
    }

    private void handleSendSms(JSONObject json) {
        String phone = json.getStr("phone");
        String content = json.getStr("content");
        log.info("【短信发送模拟】手机号：{}，内容：{}", phone, content);
    }

    private void handleSendDingTalk(JSONObject json) {
        String userId = json.getStr("userId");
        String content = json.getStr("content");
        log.info("【钉钉发送模拟】用户ID：{}，内容：{}", userId, content);
    }
}
