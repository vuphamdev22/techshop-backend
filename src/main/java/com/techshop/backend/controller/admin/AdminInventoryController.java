package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.InventoryStockRequest;
import com.techshop.backend.dto.response.admin.InventoryResponse;
import com.techshop.backend.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public List<InventoryResponse> getInventory() {
        return inventoryService.getInventory();
    }

    @PutMapping("/{productId}/stock")
    public InventoryResponse updateStock(@PathVariable Long productId,
                                         @RequestBody InventoryStockRequest request) {
        return inventoryService.updateStock(productId, request);
    }
}
