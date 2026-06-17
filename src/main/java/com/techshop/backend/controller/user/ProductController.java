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

    // tìm kiếm sản phẩm (MUST be first to avoid conflict with /{id})
    @GetMapping("/search")
    public List<ProductResponse > searchProduct(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, defaultValue = "relevance") String sort
    ){
        return productService.searchProduct(keyword, categoryId, minPrice, maxPrice, sort);
    }

    // lấy sản phẩm theo category (MUST be before /{id})
    @GetMapping("/category/{categoryId}")
    public List<ProductResponse > getProductsByCategory(@PathVariable Long categoryId){
        return productService.getProductsByCategory(categoryId);
    }

    // lấy tất cả sản phẩm
    @GetMapping
    public List<ProductResponse> getAllProducts(){
        return productService.getAllProducts();
    }

    // lấy sản phẩm theo id (MUST be last due to path variable)
    @GetMapping("/{id}")
    public ProductResponse  getProductById(@PathVariable Long id){
        return productService.getProductById(id);
    }
}
