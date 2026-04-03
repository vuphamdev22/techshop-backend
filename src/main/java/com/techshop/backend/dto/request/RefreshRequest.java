package com.techshop.backend.dto.request;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}