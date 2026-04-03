package com.techshop.backend.dto.response;

import lombok.Data;

@Data
public class OrderItemResponse {

    private Long productId;
    private String productName;
    private Double price;
    private Integer quantity;
    private String image;
}
