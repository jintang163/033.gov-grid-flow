package com.gov.grid.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.gov.grid.annotation.AuditLog;
import com.gov.grid.entity.AuditLog;
import com.gov.grid.security.JwtAuthenticationTokenFilter;
import com.gov.grid.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(1)
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog auditLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        LocalDateTime createdAt = LocalDateTime.now();

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        AuditLog logEntity = new AuditLog();
        logEntity.setCreatedAt(createdAt);

        if (request != null) {
            logEntity.setIpAddress(getClientIp(request));
            logEntity.setRequestUri(request.getRequestURI());
            logEntity.setMethod(request.getMethod());
            logEntity.setUserAgent(request.getHeader("User-Agent"));

            String eventId = request.getParameter("eventId");
            if (StrUtil.isBlank(eventId)) {
                eventId = request.getHeader("X-Event-Id");
            }
            logEntity.setEventId(eventId);
        }

        Long userId = JwtAuthenticationTokenFilter.getCurrentUserId();
        String username = JwtAuthenticationTokenFilter.getCurrentUsername();
        logEntity.setUserId(userId);
        logEntity.setUsername(username);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        logEntity.setModule(auditLog.module());
        logEntity.setOperation(auditLog.operation());
        logEntity.setDescription(auditLog.description());

        if (auditLog.recordParams()) {
            try {
                Object[] args = joinPoint.getArgs();
                String params = Arrays.stream(args)
                        .filter(arg -> !(arg instanceof javax.servlet.http.HttpServletRequest)
                                && !(arg instanceof javax.servlet.http.HttpServletResponse))
                        .map(arg -> {
                            try {
                                return JSONUtil.toJsonStr(arg);
                            } catch (Exception e) {
                                return arg != null ? arg.toString() : "null";
                            }
                        })
                        .collect(Collectors.joining(", "));
                logEntity.setRequestParams(params);
            } catch (Exception e) {
                logEntity.setRequestParams("参数解析失败");
            }
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            logEntity.setStatus(1);

            if (auditLog.recordResult()) {
                try {
                    logEntity.setResponseResult(JSONUtil.toJsonStr(result));
                } catch (Exception e) {
                    logEntity.setResponseResult("响应解析失败");
                }
            }
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setCostTime(costTime);

            try {
                auditLogService.saveLog(logEntity);
            } catch (Exception e) {
                // ignore
            }
        }

        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
