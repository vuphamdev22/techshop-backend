package com.techshop.backend.repository;

import com.techshop.backend.entity.ProductSpec;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {
}
