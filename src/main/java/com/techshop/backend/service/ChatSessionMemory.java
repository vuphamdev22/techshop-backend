package com.techshop.backend.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatSessionMemory {

    public static class SessionContext {
        private final List<String> history = new ArrayList<>();
        private final Map<String, String> attributes = new HashMap<>();

        public List<String> getHistory() {
            return history;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void addMessage(String sender, String text) {
            history.add(sender + ": " + text);
            // Keep memory bounded to avoid excessive context tokens
            if (history.size() > 20) {
                history.remove(0);
            }
        }

        public void clear() {
            history.clear();
            attributes.clear();
        }
    }

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public SessionContext getOrCreateSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = "default-session";
        }
        return sessions.computeIfAbsent(sessionId, k -> new SessionContext());
    }

    public void clearSession(String sessionId) {
        if (sessionId != null) {
            sessions.remove(sessionId);
        }
    }
}
