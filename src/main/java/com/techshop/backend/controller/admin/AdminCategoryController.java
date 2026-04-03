package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.CategoryRequest;
import com.techshop.backend.dto.response.CategoryResponse;
import com.techshop.backend.entity.Category;
import com.techshop.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService service;

    @PostMapping
    public CategoryResponse create(@RequestBody CategoryRequest request){
        return service.create(request);
    }

    @GetMapping
    public List<CategoryResponse> getAll(){
        return service.getAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse getById(@PathVariable Long id){
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @RequestBody CategoryRequest request){
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}