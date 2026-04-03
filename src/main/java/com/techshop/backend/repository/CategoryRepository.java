package com.techshop.backend.repository;


import com.techshop.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // tìm category theo tên
    Optional<Category> findByName(String name);

    // kiểm tra category đã tồn tại chưa
    boolean existsByName(String name);
}