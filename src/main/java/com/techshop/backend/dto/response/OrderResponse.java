package com.techshop.backend.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class OrderResponse {

    private Long orderId;
    private Double totalPrice;
    private String status;

    private ShippingAddressResponse shippingAddress;
    private List<OrderItemResponse> items;

    private String createdAt;

    // 🔥 thêm 2 field này
    private String paymentMethod;
    private Boolean isPaid;

    // 🔥 thêm paymentUrl (chỉ khi checkout online)
    private String paymentUrl;
    
    // 🔥 QR code URL cho thanh toán ngân hàng
    private String qrCodeUrl;
}