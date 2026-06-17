package com.techshop.backend.entity;

import com.techshop.backend.enums.MembershipLevel;
import com.techshop.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;


    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    private String address;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MembershipLevel membershipLevel = MembershipLevel.BRONZE;

    @Builder.Default
    @Column(nullable = false)
    private Integer rewardPoints = 0;

    @Builder.Default
    @Column(nullable = false)
    private Double totalSpent = 0.0;

    @Builder.Default
    @Column(nullable = false)
    private Integer totalOrders = 0;

    @Builder.Default
    @Column(nullable = false)
    private Integer exp = 0;

    @Builder.Default
    private boolean enabled = true;

    private java.time.LocalDate birthday;

    @Enumerated(EnumType.STRING)
    private com.techshop.backend.enums.Gender gender;

    private LocalDateTime lastLoginAt;
    private LocalDateTime lastOrderAt;
    private LocalDateTime firstOrderAt;

    @Builder.Default
    private boolean emailSubscribed = true;

    private LocalDateTime lastMarketingEmailSentAt;

    private LocalDateTime createdAt;
}