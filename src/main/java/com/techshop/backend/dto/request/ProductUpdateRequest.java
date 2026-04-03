package com.techshop.backend.dto.request;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ProductUpdateRequest {

    @NotBlank
    private String name;

    private String description;

    @Min(0)
    private Double price;

    private Double originalPrice;

    @Min(0)
    private Integer stock;

    private Long categoryId;

    private String badge;

    private List<String> images; // optional

    private Map<String, String> specs;
}