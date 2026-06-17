package com.techshop.backend.service;

import java.util.List;

public interface GeminiService {
    String generateContent(String systemInstruction, String prompt, List<String> history);
}
