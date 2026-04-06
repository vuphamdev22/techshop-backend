package com.techshop.backend.dto.request;

import lombok.Data;

@Data
public class InventoryStockRequest {
    private Integer stock;

    private Integer minStock;

    private Integer maxStock;
}
