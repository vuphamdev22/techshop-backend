package com.techshop.backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderSummaryResponse {
    private List<OrderSummary> orders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummary {
        private String id;
        private String customer;
        private String email;
        private String product; // Có lẽ là tên sản phẩm đầu tiên hoặc summary
        private double amount;
        private String status;
        private String date;
        private int items;
    }
}