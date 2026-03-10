package com.roadmap.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roadmap.dto.LocationDTO;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LocationWebSocketHandler.class);

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> subscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public LocationWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.debug("WebSocket connected: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = parseUserId(message.getPayload());
        if (userId != null) {
            subscriptions.put(session.getId(), userId);
            log.debug("Session {} subscribed to user {}", session.getId(), userId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        subscriptions.remove(session.getId());
        log.debug("WebSocket disconnected: {}", session.getId());
    }

    public void broadcastLocation(Long userId, LocationDTO location) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(location);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize location payload", e);
            return;
        }

        subscriptions.forEach((sessionId, subscribedUser) -> {
            if (userId.equals(subscribedUser)) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        log.warn("Failed to send location update to session {}", sessionId, e);
                    }
                }
            }
        });
    }

    private Long parseUserId(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            if (payload.trim().startsWith("{")) {
                Map<?, ?> data = objectMapper.readValue(payload, Map.class);
                Object userIdObj = data.get("userId");
                if (userIdObj != null) {
                    return Long.parseLong(String.valueOf(userIdObj));
                }
                return null;
            }
            return Long.parseLong(payload.trim());
        } catch (Exception e) {
            log.debug("Invalid subscription message: {}", payload);
            return null;
        }
    }
}
