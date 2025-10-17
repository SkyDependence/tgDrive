package com.skydevs.tgdrive.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Component
public class UploadProgressWebSocketHandler implements WebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionLocks = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService broadcastExecutor = Executors.newFixedThreadPool(4);

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        sessionLocks.put(sessionId, new Object());
        log.info("WebSocket连接建立: {}", sessionId);
    }

    @Override
    public void handleMessage(@NotNull WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 处理客户端发送的消息，比如注册文件上传ID或心跳
        String payload = message.getPayload().toString();
        log.info("收到WebSocket- {} 消息: {}",session.getId(), payload);

        try {
            // 尝试解析为JSON，处理心跳消息
            Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);
            String type = (String) messageMap.get("type");

            if ("ping".equals(type)) {
                // 响应心跳
                Map<String, String> pongMessage = new HashMap<>();
                pongMessage.put("type", "pong");
                pongMessage.put("timestamp", String.valueOf(System.currentTimeMillis()));

                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongMessage)));
                }
            }
        } catch (Exception e) {
            // 如果不是JSON格式或解析失败，记录但不中断
            log.debug("消息非JSON格式或不需要特殊处理: {}", payload);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", session.getId(), exception);
        sessions.remove(session.getId());
        sessionLocks.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessions.remove(session.getId());
        sessionLocks.remove(session.getId());
        log.info("WebSocket连接关闭: {}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }



    /**
     * 发送上传进度
     */
    public void sendUploadProgress(String fileName, double percentage) {
        UploadProgressMessage message = new UploadProgressMessage();
        message.setFileName(fileName);
        message.setPercentage(percentage);
        message.setType("upload_progress");

        broadcastMessage(message);
    }

    public void sendUploadProgress(String fileName, double percentage, int currentChunk, int totalChunks) {
        UploadProgressMessage message = new UploadProgressMessage();
        message.setFileName(fileName);
        message.setPercentage(percentage);
        message.setCurrentChunk(currentChunk);
        message.setTotalChunks(totalChunks);
        message.setType("upload_progress");

        broadcastMessage(message);
    }

    /**
     * 发送上传完成消息
     */
    public void sendUploadComplete(String fileName) {
        UploadCompleteMessage message = new UploadCompleteMessage();
        message.setFileName(fileName);
        message.setType("upload_complete");

        broadcastMessage(message);
    }

    /**
     * 发送上传失败消息
     */
    public void sendUploadError(String fileName, String error) {
        UploadErrorMessage message = new UploadErrorMessage();
        message.setFileName(fileName);
        message.setError(error);
        message.setType("upload_error");

        broadcastMessage(message);
    }

    private void broadcastMessage(Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sessions.values().forEach(session -> broadcastExecutor.execute(() -> sendMessageSafely(session, json)));
        } catch (Exception e) {
            log.error("广播WebSocket消息失败", e);
        }
    }

    private void sendMessageSafely(WebSocketSession session, String payload) {
        Object lock = sessionLocks.getOrDefault(session.getId(), session);
        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            } catch (IOException | IllegalStateException e) {
                log.error("发送WebSocket消息失败，session: {}", session.getId(), e);
            }
        }
    }

    // 消息类定义
    @Data
    public static class UploadProgressMessage {
        private String type;
        private String fileName;
        private double percentage;
        private int currentChunk;
        private int totalChunks;
    }

    @Data
    public static class UploadCompleteMessage {
        private String type;
        private String fileName;
    }

    @Data
    public static class UploadErrorMessage {
        private String type;
        private String fileName;
        private String error;
    }
}
