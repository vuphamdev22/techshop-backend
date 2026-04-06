package com.techshop.backend.service;

import com.techshop.backend.dto.request.InventoryStockRequest;
import com.techshop.backend.dto.response.admin.InventoryResponse;

import java.util.List;

public interface InventoryService {
    List<InventoryResponse> getInventory();
    InventoryResponse updateStock(Long productId, InventoryStockRequest request);
}
