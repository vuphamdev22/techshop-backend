package com.techshop.backend.service.impl;

import com.techshop.backend.dto.request.InventoryStockRequest;
import com.techshop.backend.dto.response.admin.InventoryResponse;
import com.techshop.backend.entity.Product;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.ProductMapper;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductRepository productRepository;

    private static final int FALLBACK_MIN = 10;
    private static final int FALLBACK_MAX = 100;

    @Override
    public List<InventoryResponse> getInventory() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public InventoryResponse updateStock(Long productId, InventoryStockRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.getStock() < 0) {
            throw new AppException(ErrorCode.INVALID_STOCK_VALUE);
        }

        if (request.getMinStock() != null && request.getMaxStock() != null && request.getMaxStock() < request.getMinStock()) {
            throw new AppException(ErrorCode.INVALID_STOCK_VALUE);
        }

        Integer previousStock = product.getStock() != null ? product.getStock() : 0;
        product.setStock(request.getStock());
        if (request.getStock() > previousStock) {
            product.setLastRestocked(LocalDate.now());
        }

        if (request.getMinStock() != null) {
            product.setMinStock(request.getMinStock());
        }
        if (request.getMaxStock() != null) {
            product.setMaxStock(request.getMaxStock());
        }

        Product updated = productRepository.save(product);
        return toResponse(updated);
    }

    private InventoryResponse toResponse(Product product) {
        InventoryResponse response = new InventoryResponse();
        response.setProductId(product.getId());
        response.setProduct(product.getName());
        response.setSku(product.getSku());
        response.setCategory(product.getCategory() != null ? product.getCategory().getName() : null);
        int currentStock = product.getStock() != null ? product.getStock() : 0;
        response.setCurrentStock(currentStock);
        response.setMinStock(product.getMinStock() != null ? product.getMinStock() : FALLBACK_MIN);
        response.setMaxStock(product.getMaxStock() != null ? product.getMaxStock() : Math.max(FALLBACK_MAX, currentStock));
        response.setLastRestocked(product.getLastRestocked() != null ? product.getLastRestocked().toString() : null);
        response.setStatus(determineStatus(currentStock, response.getMinStock()));
        String imageUrl = product.getImages() != null && !product.getImages().isEmpty() 
            ? product.getImages().get(0).getImageUrl() 
            : "https://via.placeholder.com/48?text=No+Image";
        return response;
    }

    private String determineStatus(int stock, Integer minStock) {
        int threshold = minStock != null ? minStock : FALLBACK_MIN;
        if (stock <= 0) {
            return "out_of_stock";
        }
        if (stock < threshold) {
            return "low_stock";
        }
        return "in_stock";
    }
}
