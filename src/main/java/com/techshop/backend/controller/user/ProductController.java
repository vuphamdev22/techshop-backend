package com.techshop.backend.controller.user;


import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.Product;
import com.techshop.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // lấy tất cả sản phẩm
    @GetMapping
    public List<ProductResponse> getAllProducts(){
        return productService.getAllProducts();
    }

    // lấy sản phẩm theo id
    @GetMapping("/{id}")
    public ProductResponse  getProductById(@PathVariable Long id){
        return productService.getProductById(id);
    }

    // lấy sản phẩm theo category
    @GetMapping("/category/{categoryId}")
    public List<ProductResponse > getProductsByCategory(@PathVariable Long categoryId){
        return productService.getProductsByCategory(categoryId);
    }

    // tìm kiếm sản phẩm
    @GetMapping("/search")
    public List<ProductResponse > searchProduct(@RequestParam String keyword){
        return productService.searchProduct(keyword);
    }
}
