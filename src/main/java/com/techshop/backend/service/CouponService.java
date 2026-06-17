package com.techshop.backend.service;

import com.techshop.backend.dto.request.CouponRequest;
import com.techshop.backend.dto.response.CouponResponse;

import java.util.List;

public interface CouponService {
    List<CouponResponse> getAllCoupons();
    List<CouponResponse> getAllCouponsForAdmin();
    CouponResponse getCouponById(Long id);
    CouponResponse getCouponByCode(String code);
    CouponResponse createCoupon(CouponRequest request);
    CouponResponse updateCoupon(Long id, CouponRequest request);
    void deleteCoupon(Long id);
    CouponResponse toggleStatus(Long id, Boolean active);
}
