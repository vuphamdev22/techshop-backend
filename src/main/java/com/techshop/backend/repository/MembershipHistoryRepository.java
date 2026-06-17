package com.techshop.backend.repository;

import com.techshop.backend.entity.MembershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MembershipHistoryRepository extends JpaRepository<MembershipHistory, Long> {
    List<MembershipHistory> findByUserIdOrderByUpgradedAtDesc(Long userId);
}
