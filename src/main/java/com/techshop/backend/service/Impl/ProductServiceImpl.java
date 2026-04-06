package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.ProductRequest;
import com.techshop.backend.dto.request.ProductUpdateRequest;
import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.Category;
import com.techshop.backend.entity.Product;
import com.techshop.backend.entity.ProductImage;
import com.techshop.backend.entity.ProductSpec;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.ProductMapper;
import com.techshop.backend.repository.CategoryRepository;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    private static final int DEFAULT_MIN_STOCK = 10;
    private static final int DEFAULT_MAX_STOCK = 100;

    @Override
    public ProductResponse createProduct(ProductRequest request){

        // 🔹 1. Lấy category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        // 🔹 2. Tạo product
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        Integer previousStock = product.getStock();
        product.setStock(request.getStock());
        product.setBadge(request.getBadge());
        product.setSku(request.getSku());
        applyInventoryDefaults(product, request.getMinStock(), request.getMaxStock());
        refreshLastRestocked(product, previousStock, request.getStock());
        product.setSku(request.getSku());
        product.setCategory(category);
        product.setStock(request.getStock());
        applyInventoryDefaults(product, request.getMinStock(), request.getMaxStock());
        if (product.getStock() != null && product.getStock() > 0) {
            product.setLastRestocked(LocalDate.now());
        }

        // =========================
        // 🔹 3. IMAGES
        // =========================
        List<ProductImage> images = new ArrayList<>();

        if (request.getImages() != null) {
            for (String url : request.getImages()) {
                ProductImage img = new ProductImage();
                img.setImageUrl(url);
                img.setProduct(product); // 🔥 bắt buộc

                images.add(img);
            }
        }

        product.setImages(images);

        // =========================
        // 🔹 4. SPECS (QUAN TRỌNG)
        // =========================
        List<ProductSpec> specs = new ArrayList<>();

        if (request.getSpecs() != null) {
            request.getSpecs().forEach((key, value) -> {
                ProductSpec spec = new ProductSpec();
                spec.setSpecKey(key);
                spec.setSpecValue(value);
                spec.setProduct(product); // 🔥 bắt buộc

                specs.add(spec);
            });
        }

        product.setSpecs(specs);

        // 🔹 5. Save
        Product saved = productRepository.save(product);

        // 🔹 6. Response
        return productMapper.toResponse(saved);
    }

    @Override
    public List<ProductResponse> getAllProducts() {

        return productRepository.findAll()
                .stream().map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse getProductById(Long id) {

        Product product =  productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toResponse(product);
    }

    @Override
    public List<ProductResponse> getProductsByCategory(Long categoryId) {

        return productRepository.findByCategoryId(categoryId)
                .stream().map(productMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProductResponse> searchProduct(String keyword) {

        return productRepository.findByNameContaining(keyword)
                .stream().map(productMapper::toResponse)
                .toList();
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStock(request.getStock());
        product.setBadge(request.getBadge());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }

        if (request.getImages() != null) {
            product.getImages().clear();

            List<ProductImage> newImages = request.getImages().stream()
                    .map(url -> {
                        ProductImage img = new ProductImage();
                        img.setImageUrl(url);
                        img.setProduct(product);
                        return img;
                    })
                    .toList();

            product.getImages().addAll(newImages);
        }
        // 🔥 specs (THIẾU CHÍNH LÀ ĐÂY)
        if (request.getSpecs() != null) {
            product.getSpecs().clear();

            List<ProductSpec> newSpecs = request.getSpecs().entrySet().stream()
                    .map(entry -> {
                        ProductSpec spec = new ProductSpec();
                        spec.setSpecKey(entry.getKey());
                        spec.setSpecValue(entry.getValue());
                        spec.setProduct(product);
                        return spec;
                    })
                    .toList();

            product.getSpecs().addAll(newSpecs);
        }

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                        .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        productRepository.deleteById(id);
    }

    private void applyInventoryDefaults(Product product, Integer minStock, Integer maxStock) {
        int resolvedMin = resolveMinStock(minStock, product.getMinStock());
        int resolvedMax = resolveMaxStock(maxStock, product.getMaxStock(), resolvedMin, product.getStock());
        product.setMinStock(resolvedMin);
        product.setMaxStock(resolvedMax);
    }

    private int resolveMinStock(Integer requested, Integer existing) {
        if (requested != null) {
            return Math.max(0, requested);
        }
        if (existing != null) {
            return Math.max(0, existing);
        }
        return DEFAULT_MIN_STOCK;
    }

    private int resolveMaxStock(Integer requested, Integer existing, int minStock, Integer currentStock) {
        if (requested != null) {
            return Math.max(minStock, requested);
        }
        if (existing != null) {
            return Math.max(minStock, existing);
        }
        int stockValue = currentStock != null ? currentStock : minStock;
        return Math.max(Math.max(minStock, stockValue), DEFAULT_MAX_STOCK);
    }

    private void refreshLastRestocked(Product product, Integer previousStock, Integer newStock) {
        int oldValue = previousStock != null ? previousStock : 0;
        int updated = newStock != null ? newStock : oldValue;
        if (updated > oldValue) {
            product.setLastRestocked(LocalDate.now());
        }
    }
}
