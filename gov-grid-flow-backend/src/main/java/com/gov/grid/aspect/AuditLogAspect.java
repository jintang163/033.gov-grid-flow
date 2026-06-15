package com.gov.grid.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
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
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Aspect
@Component
@Order(1)
public class AuditLogAspect {

    @Autowired
    private AuditLogService auditLogService;

    private static final SpelExpressionParser SPEL_PARSER = new SpelExpressionParser();
    private static final LocalVariableTableParameterNameDiscoverer PARAM_NAME_DISCOVERER =
            new LocalVariableTableParameterNameDiscoverer();

    private static final String[] EVENT_ID_FIELD_NAMES = {
            "eventId", "event_id", "eventID",
            "id", "ID", "Id",
            "eventInfoId", "event_info_id"
    };

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
        }

        Long userId = getCurrentUserIdSafe();
        String username = getCurrentUsernameSafe();
        logEntity.setUserId(userId);
        logEntity.setUsername(username);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        logEntity.setModule(auditLog.module());
        logEntity.setOperation(auditLog.operation());
        logEntity.setDescription(auditLog.description());

        String extractedEventId = extractEventId(request, method, args, signature);
        logEntity.setEventId(extractedEventId);

        if (auditLog.recordParams()) {
            try {
                String params = Arrays.stream(args)
                        .filter(arg -> !(arg instanceof javax.servlet.http.HttpServletRequest)
                                && !(arg instanceof javax.servlet.http.HttpServletResponse))
                        .map(arg -> {
                            try {
                                return safeToJson(arg);
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

            if (StrUtil.isBlank(logEntity.getEventId()) && result != null) {
                String eventIdFromResult = extractEventIdFromResult(result);
                if (StrUtil.isNotBlank(eventIdFromResult)) {
                    logEntity.setEventId(eventIdFromResult);
                }
            }

            if (auditLog.recordResult()) {
                try {
                    logEntity.setResponseResult(safeToJson(result));
                } catch (Exception e) {
                    logEntity.setResponseResult("响应解析失败");
                }
            }
        } catch (Throwable e) {
            logEntity.setStatus(0);
            logEntity.setErrorMsg(truncate(e.getMessage(), 2000));
            throw e;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            logEntity.setCostTime(costTime);

            try {
                auditLogService.saveLog(logEntity);
            } catch (Exception e) {
                // ignore audit log failure to not affect business
            }
        }

        return result;
    }

    private String extractEventId(HttpServletRequest request, Method method, Object[] args, MethodSignature signature) {
        String eventId;

        eventId = tryFromRequestParams(request);
        if (StrUtil.isNotBlank(eventId)) return eventId;

        eventId = tryFromPathVariable(request, args, method, signature);
        if (StrUtil.isNotBlank(eventId)) return eventId;

        eventId = tryFromArgsFields(args);
        if (StrUtil.isNotBlank(eventId)) return eventId;

        eventId = tryFromSpelPathVariable(request);
        if (StrUtil.isNotBlank(eventId)) return eventId;

        return null;
    }

    private String tryFromRequestParams(HttpServletRequest request) {
        if (request == null) return null;
        String eventId = request.getParameter("eventId");
        if (StrUtil.isBlank(eventId)) {
            eventId = request.getParameter("event_id");
        }
        if (StrUtil.isBlank(eventId)) {
            eventId = request.getParameter("id");
        }
        if (StrUtil.isBlank(eventId)) {
            eventId = request.getHeader("X-Event-Id");
        }
        return eventId;
    }

    private String tryFromPathVariable(HttpServletRequest request, Object[] args, Method method, MethodSignature signature) {
        if (request == null) return null;
        try {
            Map<String, String> uriTemplateVars = (Map<String, String>) request.getAttribute(
                    HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
            if (uriTemplateVars != null && !uriTemplateVars.isEmpty()) {
                for (String key : uriTemplateVars.keySet()) {
                    String lowerKey = key.toLowerCase();
                    if (lowerKey.contains("event") || lowerKey.equals("id")) {
                        String val = uriTemplateVars.get(key);
                        if (StrUtil.isNotBlank(val) && isNumericOrValidId(val)) {
                            return val;
                        }
                    }
                }
                for (String key : Arrays.asList("id", "eventId")) {
                    if (uriTemplateVars.containsKey(key)) {
                        return uriTemplateVars.get(key);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            String[] paramNames = PARAM_NAME_DISCOVERER.getParameterNames(method);
            if (paramNames != null && args != null) {
                EvaluationContext context = new StandardEvaluationContext();
                for (int i = 0; i < paramNames.length; i++) {
                    if (i < args.length) {
                        context.setVariable(paramNames[i], args[i]);
                    }
                }
                for (String key : Arrays.asList("#eventId", "#event_id", "#id", "#eventID")) {
                    try {
                        Expression expr = SPEL_PARSER.parseExpression(key);
                        Object val = expr.getValue(context);
                        if (val != null && StrUtil.isNotBlank(val.toString())) {
                            return val.toString();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String tryFromArgsFields(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg == null) continue;
            if (arg instanceof HttpServletRequest || arg instanceof javax.servlet.http.HttpServletResponse) continue;

            String found = extractFromObject(arg);
            if (StrUtil.isNotBlank(found)) {
                return found;
            }
        }
        return null;
    }

    private String extractFromObject(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                for (String field : EVENT_ID_FIELD_NAMES) {
                    Object v = map.get(field);
                    if (v != null && StrUtil.isNotBlank(v.toString()) && isNumericOrValidId(v.toString())) {
                        return v.toString();
                    }
                }
                Object data = map.get("data");
                if (data != null) {
                    return extractFromObject(data);
                }
            }

            String json = safeToJson(obj);
            if (StrUtil.isBlank(json) || !json.startsWith("{")) return null;

            JSONObject jo = JSONUtil.parseObj(json);
            for (String field : EVENT_ID_FIELD_NAMES) {
                if (jo.containsKey(field)) {
                    Object v = jo.get(field);
                    if (v != null && StrUtil.isNotBlank(v.toString()) && isNumericOrValidId(v.toString())) {
                        return v.toString();
                    }
                }
            }

            if (jo.containsKey("data")) {
                Object data = jo.get("data");
                if (data != null) {
                    return extractFromObject(data);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String tryFromSpelPathVariable(HttpServletRequest request) {
        if (request == null) return null;
        String uri = request.getRequestURI();
        if (StrUtil.isBlank(uri)) return null;

        String[] segments = uri.split("/");
        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            if (StrUtil.isBlank(seg)) continue;
            String lowerSeg = seg.toLowerCase();

            if (i > 0) {
                String prev = segments[i - 1].toLowerCase();
                if ((prev.equals("event") || prev.equals("events") || prev.equals("audit-log"))
                        && isNumericOrValidId(seg)
                        && !seg.equals("query")
                        && !seg.equals("export-pdf")
                        && !seg.equals("verify")
                        && !seg.equals("approve")
                        && !seg.equals("process")
                        && !seg.equals("assign")
                        && !seg.equals("verify")
                        && !seg.equals("return")
                        && !seg.equals("report")
                        && !seg.equals("batch-sync")) {
                    return seg;
                }
            }
        }
        return null;
    }

    private String extractEventIdFromResult(Object result) {
        if (result == null) return null;
        try {
            return extractFromObject(result);
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isNumericOrValidId(String s) {
        if (StrUtil.isBlank(s)) return false;
        if (s.matches("\\d+")) return true;
        if (s.matches("[0-9a-fA-F\\-]{8,}")) return true;
        return false;
    }

    private String safeToJson(Object obj) {
        if (obj == null) return "null";
        try {
            String json = JSONUtil.toJsonStr(obj);
            if (json.length() > 10000) {
                json = json.substring(0, 10000) + "...<truncated>";
            }
            return json;
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private Long getCurrentUserIdSafe() {
        try {
            return JwtAuthenticationTokenFilter.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String getCurrentUsernameSafe() {
        try {
            return JwtAuthenticationTokenFilter.getCurrentUsername();
        } catch (Exception e) {
            return null;
        }
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
