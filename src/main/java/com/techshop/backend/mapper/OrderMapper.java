package com.techshop.backend.mapper;

import com.techshop.backend.dto.response.OrderItemResponse;
import com.techshop.backend.dto.response.OrderResponse;
import com.techshop.backend.dto.response.ShippingAddressResponse;
import com.techshop.backend.entity.Order;
import com.techshop.backend.entity.OrderItem;
import com.techshop.backend.entity.ProductImage;
import com.techshop.backend.entity.ShippingAddress;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class OrderMapper {

    public static OrderResponse toResponse(Order order) {

        OrderResponse res = new OrderResponse();

        // 🔥 basic info
        res.setOrderId(order.getId());
        res.setTotalPrice(order.getTotalPrice());
        res.setStatus(order.getStatus().name());

        // 🔥 createdAt
        if (order.getCreatedAt() != null) {
            res.setCreatedAt(
                    order.getCreatedAt().format(
                            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
                    )
            );
        }

        // 🔥 FIX CHỖ LỖI Ở ĐÂY
        if (order.getPaymentMethod() != null) {
            res.setPaymentMethod(order.getPaymentMethod().name());
        }

        res.setIsPaid(order.getIsPaid());

        // 🔥 shipping
        res.setShippingAddress(mapShipping(order.getShippingAddress()));

        // 🔥 items
        res.setItems(mapItems(order.getItems()));

        return res;
    }

    public static OrderResponse toResponseWithPaymentUrl(Order order, String paymentUrl) {
        OrderResponse res = toResponse(order);
        res.setPaymentUrl(paymentUrl);
        return res;
    }

    // ==============================
    // 🔥 PRIVATE METHODS
    // ==============================

    private static ShippingAddressResponse mapShipping(ShippingAddress shipping) {

        if (shipping == null) return null;

        ShippingAddressResponse res = new ShippingAddressResponse();
        res.setFirstName(shipping.getFirstName());
        res.setLastName(shipping.getLastName());
        res.setEmail(shipping.getEmail());
        res.setPhone(shipping.getPhone());
        res.setAddress(shipping.getAddress());
        res.setCity(shipping.getCity());
        res.setState(shipping.getState());
        res.setZipCode(shipping.getZipCode());

        return res;
    }

    private static List<OrderItemResponse> mapItems(List<OrderItem> orderItems) {

        List<OrderItemResponse> items = new ArrayList<>();

        if (orderItems == null) return items;

        for (OrderItem item : orderItems) {

            OrderItemResponse res = new OrderItemResponse();

            if (item.getProduct() != null) {

                res.setProductId(item.getProduct().getId());
                res.setProductName(item.getProduct().getName());

                // 🔥 image
                res.setImage(getFirstImage(item));
            }

            res.setPrice(item.getPrice());
            res.setQuantity(item.getQuantity());

            items.add(res);
        }

        return items;
    }

    private static String getFirstImage(OrderItem item) {

        if (item.getProduct() == null ||
                item.getProduct().getImages() == null ||
                item.getProduct().getImages().isEmpty()) {
            return null;
        }

        ProductImage img = item.getProduct().getImages().get(0);
        return img.getImageUrl();
    }
}