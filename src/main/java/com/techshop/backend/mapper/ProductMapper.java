package com.techshop.backend.mapper;

import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.Product;
import com.techshop.backend.entity.ProductImage;
import com.techshop.backend.entity.ProductSpec;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "category.name", target = "category")
    @Mapping(source = "brand.name", target = "brand")

    // ảnh chính
    @Mapping(target = "image", expression = "java(getFirstImage(product))")

    // list ảnh
    @Mapping(target = "images", expression = "java(mapImages(product.getImages()))")

    // specs
    @Mapping(target = "specs", expression = "java(mapSpecs(product.getSpecs()))")

    // inStock
    @Mapping(target = "inStock",
            expression = "java(product.getStock() != null && product.getStock() > 0)")

    ProductResponse toResponse(Product product);

    // =========================
    // CUSTOM METHODS
    // =========================

    // 👉 lấy ảnh đầu tiên
    default String getFirstImage(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().get(0).getImageUrl();
    }

    // 👉 convert list image
    default List<String> mapImages(List<ProductImage> images) {
        if (images == null) return List.of();

        return images.stream()
                .map(ProductImage::getImageUrl)
                .toList();
    }

    // 👉 convert specs -> Map
    default Map<String, String> mapSpecs(List<ProductSpec> specs) {
        if (specs == null) return Map.of();

        return specs.stream()
                .collect(Collectors.toMap(
                        ProductSpec::getSpecKey,
                        ProductSpec::getSpecValue
                ));
    }
}