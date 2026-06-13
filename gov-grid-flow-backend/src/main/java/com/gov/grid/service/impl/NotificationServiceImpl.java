package com.gov.grid.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.mapper.SysNotificationMapper;
import com.gov.grid.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    @Value("${notify.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notify.sms.access-key:}")
    private String smsAccessKey;

    @Value("${notify.sms.sign-name:网格通知}")
    private String smsSignName;

    @Value("${notify.sms.endpoint:https://dysmsapi.aliyuncs.com}")
    private String smsEndpoint;

    @Value("${notify.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notify.email.from:noreply@gov-grid.local}")
    private String emailFrom;

    @Value("${notify.app.enabled:true}")
    private boolean appEnabled;

    @Value("${notify.async:true}")
    private boolean async;

    private final SysNotificationMapper sysNotificationMapper;
    private final JavaMailSender javaMailSender;

    @Override
    public boolean sendSms(String phone, String templateCode, Map<String, Object> params) {
        if (!smsEnabled) {
            log.warn("[SMS] 短信功能未启用，模拟发送到 {}，模板：{}，参数：{}", phone, templateCode, params);
            return true;
        }
        if (StrUtil.isBlank(phone)) {
            log.warn("[SMS] 手机号为空，跳过发送");
            return false;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("PhoneNumbers", phone);
            body.put("SignName", smsSignName);
            body.put("TemplateCode", templateCode);
            body.put("TemplateParam", JSONUtil.toJsonStr(params == null ? new HashMap<>() : params));

            HttpResponse response = HttpRequest.post(smsEndpoint)
                    .header("AccessKeyId", smsAccessKey)
                    .body(JSONUtil.toJsonStr(body))
                    .timeout(10000)
                    .execute();

            if (response.isOk()) {
                Map<String, Object> resp = JSONUtil.toBean(response.body(), Map.class);
                boolean success = "OK".equals(resp.get("Code"));
                log.info("[SMS] 发送到 {} {}，响应：{}", phone, success ? "成功" : "失败", response.body());
                return success;
            } else {
                log.error("[SMS] HTTP失败，statusCode：{}，body：{}", response.getStatus(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[SMS] 发送异常，phone：{}", phone, e);
            return false;
        }
    }

    @Override
    public boolean sendEmail(String to, String subject, String content) {
        if (!emailEnabled) {
            log.warn("[EMAIL] 邮件功能未启用，模拟发送到 {}，主题：{}，内容：{}", to, subject, content);
            return true;
        }
        if (StrUtil.isBlank(to)) {
            log.warn("[EMAIL] 收件人为空，跳过发送");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            javaMailSender.send(message);
            log.info("[EMAIL] 邮件发送成功，to：{}，subject：{}", to, subject);
            return true;
        } catch (Exception e) {
            log.error("[EMAIL] 发送异常，to：{}", to, e);
            return false;
        }
    }

    @Override
    public boolean sendAppPush(Long userId, String title, String content, String type, Long bizId) {
        if (!appEnabled || userId == null) {
            log.warn("[APP] APP推送未启用或userId为空，userId：{}", userId);
            return userId != null;
        }
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType(StrUtil.isNotBlank(type) ? type : "SYSTEM");
            notification.setBizId(bizId);
            notification.setIsRead(0);
            sysNotificationMapper.insert(notification);
            log.info("[APP] 站内通知写入成功，userId：{}，title：{}", userId, title);
            return true;
        } catch (Exception e) {
            log.error("[APP] 站内通知写入异常，userId：{}", userId, e);
            return false;
        }
    }

    @Override
    public boolean sendByChannel(String channel, Long receiverId, String receiverName, String receiverPhone,
                                 String receiverEmail, String title, String content, String type, Long bizId) {
        if (StrUtil.isBlank(channel)) {
            channel = "APP";
        }
        String finalChannel = channel.toUpperCase();
        Runnable task = () -> doSendByChannel(finalChannel, receiverId, receiverName, receiverPhone,
                receiverEmail, title, content, type, bizId);

        if (async) {
            try {
                CompletableFuture.runAsync(task);
            } catch (Exception e) {
                log.error("[NOTIFY] 异步发送提交失败", e);
                task.run();
            }
            return true;
        } else {
            task.run();
            return true;
        }
    }

    private void doSendByChannel(String channel, Long receiverId, String receiverName, String receiverPhone,
                                 String receiverEmail, String title, String content, String type, Long bizId) {
        boolean allOk = true;
        StringBuilder errorBuf = new StringBuilder();

        switch (channel) {
            case "ALL":
                if (!sendSms(receiverPhone, resolveSmsTemplate(type), buildParams(title, content, receiverName))) {
                    allOk = false;
                    errorBuf.append("SMS_FAIL;");
                }
                if (!sendEmail(receiverEmail, title, content)) {
                    allOk = false;
                    errorBuf.append("EMAIL_FAIL;");
                }
                if (!sendAppPush(receiverId, title, content, type, bizId)) {
                    allOk = false;
                    errorBuf.append("APP_FAIL;");
                }
                break;
            case "SMS":
                if (!sendSms(receiverPhone, resolveSmsTemplate(type), buildParams(title, content, receiverName))) {
                    allOk = false;
                    errorBuf.append("SMS_FAIL;");
                }
                break;
            case "EMAIL":
                if (!sendEmail(receiverEmail, title, content)) {
                    allOk = false;
                    errorBuf.append("EMAIL_FAIL;");
                }
                break;
            case "APP":
            case "SYSTEM":
            default:
                if (!sendAppPush(receiverId, title, content, type, bizId)) {
                    allOk = false;
                    errorBuf.append("APP_FAIL;");
                }
                break;
        }

        if (allOk) {
            log.info("[NOTIFY] 渠道 {} 发送成功，接收人：{}", channel, receiverName);
        } else {
            log.warn("[NOTIFY] 渠道 {} 发送存在失败，错误：{}", channel, errorBuf);
        }
    }

    private String resolveSmsTemplate(String type) {
        if (type == null) return "SMS_URGE_DEFAULT";
        switch (type) {
            case "URGE_WARNING":
                return "SMS_URGE_WARNING";
            case "URGE_OVERDUE":
                return "SMS_URGE_OVERDUE";
            case "URGE_ESCALATED":
                return "SMS_URGE_ESCALATE";
            default:
                return "SMS_URGE_DEFAULT";
        }
    }

    private Map<String, Object> buildParams(String title, String content, String receiverName) {
        Map<String, Object> p = new HashMap<>();
        p.put("title", StrUtil.sub(title, 0, 30));
        p.put("receiver", StrUtil.isNotBlank(receiverName) ? receiverName : "负责人");
        return p;
    }
}
