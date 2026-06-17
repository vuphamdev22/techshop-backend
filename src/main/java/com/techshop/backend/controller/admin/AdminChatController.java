package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.AdminChatRequest;
import com.techshop.backend.dto.response.AdminChatResponse;
import com.techshop.backend.service.AdminChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AdminChatController {

    private final AdminChatService adminChatService;

    @PostMapping("/send")
    public ResponseEntity<AdminChatResponse> sendMessage(@RequestBody AdminChatRequest request) {
        return ResponseEntity.ok(adminChatService.processChatMessage(request));
    }

    @PostMapping("/context")
    public ResponseEntity<Void> clearContext(@RequestParam String sessionId) {
        adminChatService.clearContext(sessionId);
        return ResponseEntity.ok().build();
    }
}
