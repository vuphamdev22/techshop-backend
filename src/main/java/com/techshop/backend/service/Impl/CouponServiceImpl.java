package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.CouponRequest;
import com.techshop.backend.dto.response.CouponResponse;
import com.techshop.backend.entity.Coupon;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.CouponRepository;
import com.techshop.backend.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository repository;

    @Override
    public List<CouponResponse> getAllCoupons() {
        LocalDateTime now = LocalDateTime.now();
        return repository.findAll().stream()
                .filter(c -> c.getIsActive() != null && c.getIsActive())
                .filter(c -> c.getStartDate() == null || c.getStartDate().isBefore(now))
                .filter(c -> c.getEndDate() == null || c.getEndDate().isAfter(now))
                .filter(c -> c.getUsageLimit() == null || c.getUsedCount() < c.getUsageLimit())
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CouponResponse> getAllCouponsForAdmin() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CouponResponse getCouponById(Long id) {
        Coupon coupon = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        return toResponse(coupon);
    }

    @Override
    public CouponResponse getCouponByCode(String code) {
        Coupon coupon = repository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        // Validate coupon availability
        if (coupon.getIsActive() != null && !coupon.getIsActive()) {
            throw new AppException(ErrorCode.COUPON_INACTIVE);
        }

        LocalDateTime now = LocalDateTime.now();
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(now)) {
            throw new AppException(ErrorCode.COUPON_INACTIVE);
        }

        if (coupon.getEndDate() != null && coupon.getEndDate().isBefore(now)) {
            throw new AppException(ErrorCode.COUPON_EXPIRED);
        }

        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new AppException(ErrorCode.COUPON_LIMIT_REACHED);
        }

        return toResponse(coupon);
    }

    @Override
    public CouponResponse createCoupon(CouponRequest request) {
        if (repository.findByCode(request.getCode().toUpperCase()).isPresent()) {
            throw new AppException(ErrorCode.COUPON_ALREADY_EXISTS);
        }

        Coupon coupon = new Coupon();
        mapRequestToEntity(request, coupon);
        coupon.setUsedCount(0); // Initialize count to 0

        return toResponse(repository.save(coupon));
    }

    @Override
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));

        // If code changes, ensure uniqueness
        if (request.getCode() != null && !request.getCode().equalsIgnoreCase(coupon.getCode())) {
            if (repository.findByCode(request.getCode().toUpperCase()).isPresent()) {
                throw new AppException(ErrorCode.COUPON_ALREADY_EXISTS);
            }
        }

        mapRequestToEntity(request, coupon);

        return toResponse(repository.save(coupon));
    }

    @Override
    public void deleteCoupon(Long id) {
        Coupon coupon = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        repository.delete(coupon);
    }

    @Override
    public CouponResponse toggleStatus(Long id, Boolean active) {
        Coupon coupon = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COUPON_NOT_FOUND));
        coupon.setIsActive(active);
        return toResponse(repository.save(coupon));
    }

    // ================= MAPPING HELPERS =================

    private CouponResponse toResponse(Coupon coupon) {
        CouponResponse res = new CouponResponse();
        res.setId(coupon.getId());
        res.setCode(coupon.getCode());
        res.setDiscountType(coupon.getDiscountType());
        res.setDiscountValue(coupon.getDiscountValue());
        res.setMinOrderValue(coupon.getMinOrderValue());
        res.setMaxDiscount(coupon.getMaxDiscount());
        res.setUsageLimit(coupon.getUsageLimit());
        res.setUsedCount(coupon.getUsedCount());
        res.setStartDate(coupon.getStartDate());
        res.setEndDate(coupon.getEndDate());
        res.setIsActive(coupon.getIsActive());
        return res;
    }

    private void mapRequestToEntity(CouponRequest req, Coupon coupon) {
        if (req.getCode() != null) {
            coupon.setCode(req.getCode().toUpperCase());
        }
        if (req.getDiscountType() != null) {
            coupon.setDiscountType(req.getDiscountType());
        }
        if (req.getDiscountValue() != null) {
            coupon.setDiscountValue(req.getDiscountValue());
        }
        if (req.getMinOrderValue() != null) {
            coupon.setMinOrderValue(req.getMinOrderValue());
        }
        if (req.getMaxDiscount() != null) {
            coupon.setMaxDiscount(req.getMaxDiscount());
        }
        if (req.getUsageLimit() != null) {
            coupon.setUsageLimit(req.getUsageLimit());
        }
        if (req.getStartDate() != null) {
            coupon.setStartDate(req.getStartDate());
        }
        if (req.getEndDate() != null) {
            coupon.setEndDate(req.getEndDate());
        }
        if (req.getIsActive() != null) {
            coupon.setIsActive(req.getIsActive());
        }
    }
}
