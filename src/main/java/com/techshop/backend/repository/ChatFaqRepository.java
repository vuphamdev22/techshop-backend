package com.techshop.backend.repository;

import com.techshop.backend.entity.ChatFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatFaqRepository extends JpaRepository<ChatFaq, Long> {
    List<ChatFaq> findByCategory(String category);
}
