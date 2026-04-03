package com.techshop.backend.dto.response;

import lombok.Data;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;

    // optional (sau này dùng cho UI)
    private Long productCount;
}