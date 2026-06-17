package com.techshop.backend.controller.user;

import com.techshop.backend.dto.response.BrandResponse;
import com.techshop.backend.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // Get all brands
    @GetMapping
    public List<BrandResponse> getAllBrands() {
        return brandService.getAll();
    }

    // Get brand by id
    @GetMapping("/{id}")
    public BrandResponse getBrandById(@PathVariable Long id) {
        return brandService.getById(id);
    }
}
