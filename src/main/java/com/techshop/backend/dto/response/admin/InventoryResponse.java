package com.techshop.backend.dto.response.admin;

import lombok.Data;

@Data
public class InventoryResponse {
    private Long productId;
    private String product;
    private String sku;
    private String category;
    private Integer currentStock;
    private Integer minStock;
    private Integer maxStock;
    private String lastRestocked;
    private String status;
    private String imageUrl;
}
