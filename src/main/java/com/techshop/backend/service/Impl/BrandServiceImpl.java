package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.BrandRequest;
import com.techshop.backend.dto.response.BrandResponse;
import com.techshop.backend.entity.Brand;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.BrandMapper;
import com.techshop.backend.repository.BrandRepository;
import com.techshop.backend.repository.ProductRepository;
import com.techshop.backend.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository repository;
    private final ProductRepository productRepository;
    private final BrandMapper mapper;

    @Override
    public BrandResponse create(BrandRequest request) {
        if (repository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.INVALID_STATUS); // Or another custom error if name is duplicate
        }

        Brand brand = mapper.toEntity(request);
        Brand saved = repository.save(brand);

        return mapper.toResponse(saved);
    }

    @Override
    public List<BrandResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(brand -> {
                    BrandResponse res = mapper.toResponse(brand);
                    long count = productRepository.countByBrandId(brand.getId());
                    res.setProductCount(count);
                    return res;
                })
                .toList();
    }

    @Override
    public BrandResponse getById(Long id) {
        Brand brand = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        BrandResponse res = mapper.toResponse(brand);
        long count = productRepository.countByBrandId(id);
        res.setProductCount(count);

        return res;
    }

    @Override
    public BrandResponse update(Long id, BrandRequest request) {
        Brand brand = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        if (request.getName() != null) {
            brand.setName(request.getName());
        }
        if (request.getDescription() != null) {
            brand.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            brand.setLogoUrl(request.getLogoUrl());
        }
        if (request.getActive() != null) {
            brand.setActive(request.getActive());
        }

        return mapper.toResponse(repository.save(brand));
    }

    @Override
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new AppException(ErrorCode.BRAND_NOT_FOUND);
        }
        if (productRepository.countByBrandId(id) > 0) {
            throw new AppException(ErrorCode.BRAND_HAS_PRODUCTS);
        }
        repository.deleteById(id);
    }
}
