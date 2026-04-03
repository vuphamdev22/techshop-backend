package com.techshop.backend.entity;

import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 ORDER (OneToOne)
    @OneToOne
    @JoinColumn(name = "order_id", unique = true)
    private Order order;

    // 💰 phương thức thanh toán
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;

    // 📊 trạng thái thanh toán
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    // 🔢 mã giao dịch (từ cổng thanh toán)
    private String transactionId;

    // 💵 số tiền
    private BigDecimal amount;

    // ⏱ thời gian tạo
    private LocalDateTime createdAt;

    // ⏱ thời gian cập nhật
    private LocalDateTime updatedAt;

    // 📝 ghi chú (optional)
    private String note;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
