package com.techshop.backend.service;

import com.techshop.backend.dto.request.BrandRequest;
import com.techshop.backend.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse create(BrandRequest request);

    List<BrandResponse> getAll();

    BrandResponse getById(Long id);

    BrandResponse update(Long id, BrandRequest request);

    void delete(Long id);
}
