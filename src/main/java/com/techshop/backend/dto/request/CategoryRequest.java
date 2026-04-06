package com.techshop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.techshop.backend.enums.CategoryStatus;

@Data
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    private String icon;

    private CategoryStatus status = CategoryStatus.ACTIVE;
}
