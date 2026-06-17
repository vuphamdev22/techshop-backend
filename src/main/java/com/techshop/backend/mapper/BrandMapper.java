package com.techshop.backend.mapper;

import com.techshop.backend.dto.request.BrandRequest;
import com.techshop.backend.dto.response.BrandResponse;
import com.techshop.backend.entity.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toEntity(BrandRequest request);

    @Mapping(source = "logoUrl", target = "logo")
    BrandResponse toResponse(Brand brand);
}
