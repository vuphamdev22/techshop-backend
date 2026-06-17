package com.techshop.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BrandRequest {

    @NotBlank(message = "Brand name is required")
    private String name;

    private String description;

    private String logoUrl;

    private Boolean active = true;
}
