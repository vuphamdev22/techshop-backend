package com.techshop.backend.dto.request;

import com.techshop.backend.enums.MembershipLevel;
import com.techshop.backend.enums.Gender;
import lombok.Data;

@Data
public class CampaignFilterRequest {
    private MembershipLevel membershipLevel;
    private Double minTotalSpent;
    private Integer inactiveDays;
    private Gender gender;
    private Boolean emailSubscribedOnly;
}
