package com.techshop.backend.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class ProductResponse {

    private Long id;

    private String name;

    private String category; // chỉ lấy name thôi (frontend cần string)

    private Double price;
    private Double originalPrice;

    private Integer stock;

    private String sku;
    private Integer minStock;
    private Integer maxStock;
    private LocalDate lastRestocked;

    private Double rating;
    private Integer reviews;

    private String image; // ảnh chính

    private List<String> images; // tất cả ảnh

    private String badge;

    private String brand;

    private String description;

    private Map<String, String> specs;

    private boolean inStock;
}
