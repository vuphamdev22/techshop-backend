package com.techshop.backend.controller.user;

import com.techshop.backend.dto.response.CouponResponse;
import com.techshop.backend.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService service;

    @GetMapping
    public List<CouponResponse> getAllCoupons() {
        return service.getAllCoupons();
    }

    @GetMapping("/{id}")
    public CouponResponse getCouponById(@PathVariable Long id) {
        return service.getCouponById(id);
    }

    @GetMapping("/code/{code}")
    public CouponResponse getCouponByCode(@PathVariable String code) {
        return service.getCouponByCode(code);
    }
}
