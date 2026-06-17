package com.techshop.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@Setter
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String discountType; // "percentage" or "fixed"

    @Column(nullable = false)
    private Double discountValue;

    private Double minOrderValue;

    private Double maxDiscount;

    private Integer usageLimit;

    @Column(nullable = false)
    private Integer usedCount = 0;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Column(nullable = false)
    private Boolean isActive = true;
}
