package com.techshop.backend.entity;

import com.techshop.backend.enums.OrderStatus;
import com.techshop.backend.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 USER
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 💰 tổng tiền
    private Double totalPrice;

    // 🚚 địa chỉ giao hàng
    @Embedded
    private ShippingAddress shippingAddress;

    // 📦 trạng thái đơn
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // ⏱ thời gian tạo
    private LocalDateTime createdAt;

    // 🔥 PAYMENT (NEW - không ảnh hưởng logic cũ)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;   // COD, VNPAY, MOMO
    private Boolean isPaid;         // true/false
    private String paymentTxnId;    // mã giao dịch bên cổng thanh toán

    // 🔗 ITEMS
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    // 🔥 auto set thời gian + default payment
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();

        // default (an toàn)
        if (this.isPaid == null) {
            this.isPaid = false;
        }
    }
}