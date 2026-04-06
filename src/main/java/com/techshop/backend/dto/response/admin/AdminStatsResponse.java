package com.techshop.backend.dto.response.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {
    private StatItem revenue;
    private StatItem orders;
    private StatItem users;
    private StatItem products;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatItem {
        private double value;
        private double change;
        private String period;
    }
}