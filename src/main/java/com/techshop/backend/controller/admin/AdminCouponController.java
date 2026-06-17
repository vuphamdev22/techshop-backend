package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.CouponRequest;
import com.techshop.backend.dto.response.CouponResponse;
import com.techshop.backend.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponService service;

    @PostMapping
    public CouponResponse create(@RequestBody CouponRequest request) {
        return service.createCoupon(request);
    }

    @GetMapping
    public List<CouponResponse> getAll() {
        return service.getAllCouponsForAdmin();
    }

    @GetMapping("/{id}")
    public CouponResponse getById(@PathVariable Long id) {
        return service.getCouponById(id);
    }

    @PutMapping("/{id}")
    public CouponResponse update(@PathVariable Long id, @RequestBody CouponRequest request) {
        return service.updateCoupon(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.deleteCoupon(id);
    }

    @PatchMapping("/{id}/status")
    public CouponResponse toggleStatus(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        return service.toggleStatus(id, active);
    }
}
