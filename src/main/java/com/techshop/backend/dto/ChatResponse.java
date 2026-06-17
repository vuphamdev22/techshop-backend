package com.techshop.backend.dto;

import com.techshop.backend.dto.response.ProductResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class ChatResponse {
    private String text;
    private List<ProductResponse> products;
    private List<String> quickReplies;
}
