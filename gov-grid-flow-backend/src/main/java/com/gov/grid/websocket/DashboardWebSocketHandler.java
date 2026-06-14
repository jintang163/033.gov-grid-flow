package com.gov.grid.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.grid.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SESSIONS.put(session.getId(), session);
        log.info("Dashboard WebSocket connected: {}", session.getId());
        try {
            Map<String, Object> data = dashboardService.getDashboardAllData();
            String payload = objectMapper.writeValueAsString(Map.of("type", "init", "data", data));
            session.sendMessage(new TextMessage(payload));
        } catch (Exception e) {
            log.error("Failed to send initial data to session {}", session.getId(), e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSIONS.remove(session.getId());
        log.info("Dashboard WebSocket disconnected: {}, status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
        SESSIONS.remove(session.getId());
    }

    @Scheduled(fixedRate = 30000)
    public void pushDashboardData() {
        if (SESSIONS.isEmpty()) {
            return;
        }
        try {
            Map<String, Object> data = dashboardService.getDashboardAllData();
            String payload = objectMapper.writeValueAsString(Map.of("type", "update", "data", data));
            TextMessage message = new TextMessage(payload);
            for (Map.Entry<String, WebSocketSession> entry : SESSIONS.entrySet()) {
                WebSocketSession session = entry.getValue();
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.error("Failed to send data to session {}", entry.getKey(), e);
                        SESSIONS.remove(entry.getKey());
                    }
                } else {
                    SESSIONS.remove(entry.getKey());
                }
            }
        } catch (Exception e) {
            log.error("Failed to push dashboard data", e);
        }
    }
}
