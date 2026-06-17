package com.techshop.backend.entity;

import com.techshop.backend.enums.MembershipLevel;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "membership_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private MembershipLevel fromLevel;

    @Enumerated(EnumType.STRING)
    private MembershipLevel toLevel;

    private String note;

    @Builder.Default
    private LocalDateTime upgradedAt = LocalDateTime.now();
}
