package com.techshop.backend.dto.response;

import lombok.Data;
import com.techshop.backend.enums.CategoryStatus;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String icon;
    private CategoryStatus status;

    // optional (sau này dùng cho UI)
    private Long productCount;
}