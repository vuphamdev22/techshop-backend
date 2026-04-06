package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.response.admin.*;
import com.techshop.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public AdminStatsResponse getStats() {
        return adminDashboardService.getStats();
    }

    @GetMapping("/revenue")
    public RevenueDataResponse getRevenueData() {
        return adminDashboardService.getRevenueData();
    }

    @GetMapping("/top-products")
    public TopProductResponse getTopProducts() {
        return adminDashboardService.getTopProducts();
    }

    @GetMapping("/orders")
    public AdminOrderSummaryResponse getRecentOrders() {
        return adminDashboardService.getRecentOrders();
    }
}