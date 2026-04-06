package com.techshop.backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private List<ProductData> products;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductData {
        private Long id;
        private String name;
        private int sales;
        private double revenue;
        private int stock;
    }
}