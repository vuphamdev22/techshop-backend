package com.techshop.backend.controller;

import com.techshop.backend.dto.ChatRequest;
import com.techshop.backend.dto.ChatResponse;
import com.techshop.backend.entity.ChatFaq;
import com.techshop.backend.service.ChatService;
import com.techshop.backend.service.ChatSessionMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class ChatController {

    private final ChatService chatService;
    private final ChatSessionMemory chatSessionMemory;

    @PostMapping("/send")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.processChatMessage(request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<String>> getHistory(@RequestParam String sessionId) {
        ChatSessionMemory.SessionContext session = chatSessionMemory.getOrCreateSession(sessionId);
        return ResponseEntity.ok(session.getHistory());
    }

    @GetMapping("/faq")
    public ResponseEntity<List<ChatFaq>> getFaqs() {
        return ResponseEntity.ok(chatService.getAllFaqs());
    }

    @PostMapping("/context")
    public ResponseEntity<Void> clearContext(@RequestParam String sessionId) {
        chatSessionMemory.clearSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/recommend")
    public ResponseEntity<ChatResponse> getRecommendations(@RequestBody ChatRequest request) {
        ChatSessionMemory.SessionContext session = chatSessionMemory.getOrCreateSession(request.getSessionId());
        // Seed manually selected category context
        session.getAttributes().put("category", request.getMessage());
        return ResponseEntity.ok(chatService.processChatMessage(request));
    }
}
