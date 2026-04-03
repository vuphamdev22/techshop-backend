package com.techshop.backend.dto.response;

import lombok.Data;

@Data
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Double price;
    private String image;
    private Integer quantity;
}
