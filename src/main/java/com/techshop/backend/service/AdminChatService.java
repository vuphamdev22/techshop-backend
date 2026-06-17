package com.techshop.backend.service;

import com.techshop.backend.dto.request.AdminChatRequest;
import com.techshop.backend.dto.response.AdminChatResponse;

public interface AdminChatService {
    AdminChatResponse processChatMessage(AdminChatRequest request);
    void clearContext(String sessionId);
}
