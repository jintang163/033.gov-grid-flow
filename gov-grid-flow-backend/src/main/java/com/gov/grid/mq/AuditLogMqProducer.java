package com.gov.grid.mq;

import cn.hutool.json.JSONUtil;
import com.gov.grid.entity.AuditLog;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMqProducer {

    private static final Logger log = LoggerFactory.getLogger(AuditLogMqProducer.class);

    public static final String AUDIT_LOG_TOPIC = "audit-log-topic";

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Value("${audit.mq-enabled:true}")
    private boolean mqEnabled;

    public void sendAuditLog(AuditLog auditLog) {
        if (!mqEnabled) {
            return;
        }
        try {
            String json = JSONUtil.toJsonStr(auditLog);
            rocketMQTemplate.sendOneWay(AUDIT_LOG_TOPIC, json);
            log.debug("审计日志已发送到RocketMQ, id={}", auditLog.getId());
        } catch (Exception e) {
            log.warn("审计日志发送RocketMQ失败, id={}, error={}", auditLog.getId(), e.getMessage());
        }
    }
}
