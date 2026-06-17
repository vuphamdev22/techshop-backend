package com.techshop.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class AdminChatResponse {
    private String text;
    private List<String> quickReplies;
    private ChatAction action;

    @Getter
    @Setter
    @Builder
    public static class ChatAction {
        private String type; // e.g. "CREATE_VOUCHER"
        private Map<String, Object> parameters;
    }
}
