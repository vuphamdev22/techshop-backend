package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.CategoryRequest;
import com.techshop.backend.dto.response.CategoryResponse;
import com.techshop.backend.entity.Category;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.CategoryMapper;
import com.techshop.backend.repository.CategoryRepository;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final ProductRepository productRepository;
    private final CategoryMapper mapper;

    @Override
    public CategoryResponse create(CategoryRequest request) {

        Category category = mapper.toEntity(request);
        Category saved = repository.save(category);

        return mapper.toResponse(saved);
    }

    @Override
    public List<CategoryResponse> getAll() {

        return repository.findAll()
                .stream()
                .map(category -> {
                    CategoryResponse res = mapper.toResponse(category);

                    long count = productRepository.countByCategoryId(category.getId());
                    res.setProductCount(count);

                    return res;
                })
                .toList();
    }

    @Override
    public CategoryResponse getById(Long id) {

        Category category = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        CategoryResponse res = mapper.toResponse(category);

        long count = productRepository.countByCategoryId(id);
        res.setProductCount(count);

        return res;
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {

        Category category = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        return mapper.toResponse(repository.save(category));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        if (productRepository.countByCategoryId(id) > 0) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }
        repository.deleteById(id);
    }
}