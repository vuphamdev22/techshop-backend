package com.techshop.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    private String sessionId;
    private String message;
    private String email; // Optional email to fetch logged-in user context
}
