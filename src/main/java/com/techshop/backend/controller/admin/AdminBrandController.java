package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.BrandRequest;
import com.techshop.backend.dto.response.BrandResponse;
import com.techshop.backend.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
public class AdminBrandController {

    private final BrandService service;

    @PostMapping
    public BrandResponse create(@RequestBody BrandRequest request){
        return service.create(request);
    }

    @GetMapping
    public List<BrandResponse> getAll(){
        return service.getAll();
    }

    @GetMapping("/{id}")
    public BrandResponse getById(@PathVariable Long id){
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public BrandResponse update(@PathVariable Long id, @RequestBody BrandRequest request){
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }
}
