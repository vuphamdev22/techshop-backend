package com.techshop.backend.dto.request;

import lombok.Data;

@Data
public class UpdateCartItemRequest {
    private Long cartItemId;
    private Integer quantity;
}