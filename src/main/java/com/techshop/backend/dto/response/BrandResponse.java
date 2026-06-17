package com.techshop.backend.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BrandResponse {

    private Long id;
    private String name;
    private String description;
    private String logoUrl;
    
    // Alias of logoUrl for frontend compatibility with @/lib/admin type
    private String logo; 
    
    private boolean active;
    private LocalDateTime createdAt;
    
    // Optional, for statistics in dashboard
    private Long productCount;
}
