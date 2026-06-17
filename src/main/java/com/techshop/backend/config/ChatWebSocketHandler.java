package com.techshop.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techshop.backend.dto.ChatRequest;
import com.techshop.backend.dto.ChatResponse;
import com.techshop.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket connection established with session ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received WebSocket message: {}", payload);

        try {
            // Parse incoming payload into ChatRequest DTO
            ChatRequest request = objectMapper.readValue(payload, ChatRequest.class);

            // 1. Broadcast "typing" status to simulate realistic thinking time
            Map<String, Object> typingStatus = new HashMap<>();
            typingStatus.put("status", "typing");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(typingStatus)));

            // Simulate typing delay (e.g. 1000ms)
            Thread.sleep(1000);

            // 2. Call ChatService to process query
            ChatResponse response = chatService.processChatMessage(request);

            // 3. Send final "done" response package with rich models
            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("status", "done");
            finalResponse.put("text", response.getText());
            finalResponse.put("products", response.getProducts());
            finalResponse.put("quickReplies", response.getQuickReplies());

            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(finalResponse)));

        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            
            // Send error event
            Map<String, Object> errorStatus = new HashMap<>();
            errorStatus.put("status", "error");
            errorStatus.put("text", "Xin lỗi bạn, VoltBot vừa gặp sự cố nhỏ khi xử lý câu hỏi. Vui lòng thử lại sau! 😅");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorStatus)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket connection closed with session ID: {}, status: {}", session.getId(), status);
    }
}
