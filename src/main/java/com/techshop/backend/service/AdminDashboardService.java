package com.techshop.backend.service;

import com.techshop.backend.dto.response.admin.AdminOrderSummaryResponse;
import com.techshop.backend.dto.response.admin.AdminStatsResponse;
import com.techshop.backend.dto.response.admin.RevenueDataResponse;
import com.techshop.backend.dto.response.admin.TopProductResponse;

public interface AdminDashboardService {
    AdminStatsResponse getStats();
    RevenueDataResponse getRevenueData();
    TopProductResponse getTopProducts();
    AdminOrderSummaryResponse getRecentOrders();
}