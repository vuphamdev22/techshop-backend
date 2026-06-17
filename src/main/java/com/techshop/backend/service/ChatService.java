package com.techshop.backend.service;

import com.techshop.backend.dto.ChatRequest;
import com.techshop.backend.dto.ChatResponse;
import com.techshop.backend.entity.ChatFaq;

import java.util.List;

public interface ChatService {
    ChatResponse processChatMessage(ChatRequest request);
    List<ChatFaq> getAllFaqs();
}
