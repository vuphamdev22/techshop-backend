package com.techshop.backend.controller.user;

import com.techshop.backend.dto.response.CategoryResponse;
import com.techshop.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // Get all categories
    @GetMapping
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAll();
    }

    // Get category by id
    @GetMapping("/{id}")
    public CategoryResponse getCategoryById(@PathVariable Long id) {
        return categoryService.getById(id);
    }
}
