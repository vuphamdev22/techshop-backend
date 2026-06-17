package com.techshop.backend.repository;

import com.techshop.backend.entity.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {
    List<UserVoucher> findByUserIdAndUsedFalse(Long userId);
    List<UserVoucher> findByUserId(Long userId);
    boolean existsByUserIdAndCouponId(Long userId, Long couponId);
}
