package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.ProductRequest;
import com.techshop.backend.dto.request.ProductUpdateRequest;
import com.techshop.backend.dto.response.ProductResponse;
import com.techshop.backend.entity.Brand;
import com.techshop.backend.entity.Category;
import com.techshop.backend.entity.Product;
import com.techshop.backend.repository.BrandRepository;
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

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductMapper productMapper;

    private static final int DEFAULT_MIN_STOCK = 10;
    private static final int DEFAULT_MAX_STOCK = 100;
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

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
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
            product.setBrand(brand);
        }
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
    public List<ProductResponse> searchProduct(
            String keyword,
            Long categoryId,
            Double minPrice,
            Double maxPrice,
            String sort
    ) {
        String normalizedKeyword = normalize(keyword);
        List<String> terms = List.of(normalizedKeyword.split("\\s+"))
                .stream()
                .filter(term -> !term.isBlank())
                .toList();

        if (terms.isEmpty()) {
            return List.of();
        }

        Comparator<Product> comparator = comparatorFor(sort, terms);

        return productRepository.findAll()
                .stream()
                .filter(product -> categoryId == null
                        || (product.getCategory() != null && categoryId.equals(product.getCategory().getId())))
                .filter(product -> minPrice == null || safePrice(product) >= minPrice)
                .filter(product -> maxPrice == null || safePrice(product) <= maxPrice)
                .filter(product -> matchesAllTerms(product, terms))
                .sorted(comparator)
                .map(productMapper::toResponse)
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
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
            product.setBrand(brand);
        }

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

    private Comparator<Product> comparatorFor(String sort, List<String> terms) {
        String normalizedSort = sort == null ? "relevance" : sort.toLowerCase(Locale.ROOT);
        return switch (normalizedSort) {
            case "price-asc" -> Comparator.comparingDouble(this::safePrice);
            case "price-desc" -> Comparator.comparingDouble(this::safePrice).reversed();
            case "rating" -> Comparator.comparingDouble(this::safeRating).reversed();
            default -> Comparator
                    .comparingInt((Product product) -> relevanceScore(product, terms))
                    .reversed()
                    .thenComparing(Product::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
    }

    private boolean matchesAllTerms(Product product, List<String> terms) {
        String searchText = searchText(product);
        return terms.stream().allMatch(searchText::contains);
    }

    private int relevanceScore(Product product, List<String> terms) {
        String name = normalize(product.getName());
        String sku = normalize(product.getSku());
        String category = product.getCategory() != null ? normalize(product.getCategory().getName()) : "";
        String description = normalize(product.getDescription());
        String fullKeyword = String.join(" ", terms);

        int score = 0;
        if (!fullKeyword.isBlank() && name.equals(fullKeyword)) score += 120;
        if (!fullKeyword.isBlank() && name.startsWith(fullKeyword)) score += 90;
        if (!fullKeyword.isBlank() && sku.equals(fullKeyword)) score += 85;
        if (!fullKeyword.isBlank() && name.contains(fullKeyword)) score += 70;

        for (String term : terms) {
            if (name.startsWith(term)) score += 30;
            if (name.contains(term)) score += 24;
            if (sku.contains(term)) score += 22;
            if (category.contains(term)) score += 14;
            if (description.contains(term)) score += 8;
        }

        score += Math.min(10, (int) Math.round(safeRating(product)));
        return score;
    }

    private String searchText(Product product) {
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "";
        return normalize(String.join(" ",
                nullToEmpty(product.getName()),
                nullToEmpty(product.getDescription()),
                nullToEmpty(product.getSku()),
                nullToEmpty(product.getBadge()),
                nullToEmpty(categoryName)
        ));
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double safePrice(Product product) {
        return product.getPrice() != null ? product.getPrice() : 0;
    }

    private double safeRating(Product product) {
        return product.getRating() != null ? product.getRating() : 0;
    }
}
