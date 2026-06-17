package com.techshop.backend.dto.response;

import com.techshop.backend.enums.MembershipLevel;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MembershipResponse {
    private Long userId;
    private String fullName;
    private String email;
    private MembershipLevel membershipLevel;
    private Integer rewardPoints;
    private Double totalSpent;
    private Integer totalOrders;
    private Integer exp;

    // Progress to next level
    private MembershipLevel nextLevel;
    private Integer ordersToNextLevel;
    private Double spentToNextLevel;
    private double progressPercent;
    private String progressLabel;
}
