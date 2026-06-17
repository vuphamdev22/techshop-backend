package com.techshop.backend.service;

import com.techshop.backend.dto.request.ProductRequest;
import com.techshop.backend.dto.request.ProductUpdateRequest;
import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.Product;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest product);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    List<ProductResponse> getProductsByCategory(Long categoryId);

    List<ProductResponse> searchProduct(
            String keyword,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            String sort
    );

    ProductResponse updateProduct(Long id, ProductUpdateRequest product);

    void deleteProduct(Long id);
}
