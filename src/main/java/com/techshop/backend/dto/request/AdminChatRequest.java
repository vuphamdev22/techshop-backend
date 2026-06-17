package com.techshop.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminChatRequest {
    private String sessionId;
    private String message;
}
