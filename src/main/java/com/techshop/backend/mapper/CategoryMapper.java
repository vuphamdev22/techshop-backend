package com.techshop.backend.mapper;

import com.techshop.backend.dto.request.CategoryRequest;
import com.techshop.backend.dto.response.CategoryResponse;
import com.techshop.backend.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toEntity(CategoryRequest request);

    CategoryResponse toResponse(Category category);

}