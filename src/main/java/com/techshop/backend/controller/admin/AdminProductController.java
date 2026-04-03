package com.techshop.backend.controller.admin;


import com.techshop.backend.dto.request.ProductRequest;
import com.techshop.backend.dto.request.ProductUpdateRequest;
import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.Product;
import com.techshop.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

    // tạo sản phẩm
    @PostMapping
    public ProductResponse createProduct(@RequestBody ProductRequest request){
        return productService.createProduct(request);
    }

    // update sản phẩm
    @PutMapping("/{id}")
    public ProductResponse  updateProduct(@PathVariable Long id,
                                 @RequestBody ProductUpdateRequest product){
        return productService.updateProduct(id, product);
    }

    // xóa sản phẩm
    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id){
        productService.deleteProduct(id);
        return "Product deleted";
    }
}