package com.techshop.backend.service;

import com.techshop.backend.dto.request.CategoryRequest;
import com.techshop.backend.dto.response.CategoryResponse;
import com.techshop.backend.entity.Category;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    List<CategoryResponse> getAll();

    CategoryResponse getById(Long id);

    CategoryResponse update(Long id, CategoryRequest request);

    void delete(Long id);
}
