package com.techshop.backend.service.Impl;

import com.techshop.backend.config.VnPayConfig;
import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.entity.Order;
import com.techshop.backend.entity.OrderItem;
import com.techshop.backend.entity.Payment;
import com.techshop.backend.entity.Product;
import com.techshop.backend.enums.OrderStatus;
import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.enums.PaymentStatus;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.PaymentMapper;
import com.techshop.backend.repository.OrderRepository;
import com.techshop.backend.repository.PaymentRepository;
import com.techshop.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final VnPayConfig vnPayConfig;

    // ================= CREATE PAYMENT =================

    @Override
    @Transactional
    public PaymentResponse createPayment(Long orderId,
                                         PaymentCreateRequest request,
                                         String ipAddress) {

        log.info("Creating payment for orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // ✅ validate order state
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATE);
        }

        // ✅ không cho COD
        if (request.getMethod() == PaymentMethod.COD) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        // ✅ tránh tạo payment duplicate
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING);

        // ✅ dùng BigDecimal
        payment.setAmount(BigDecimal.valueOf(order.getTotalPrice()));

        payment.setTransactionId("ORDER_" + order.getId());

        Payment saved = paymentRepository.save(payment);

        String paymentUrl = generateVnPayUrl(saved, ipAddress);

        log.info("Payment created txn={}", saved.getTransactionId());

        return paymentMapper.toResponseWithUrl(saved, paymentUrl);
    }

    // ================= VNPAY RETURN =================

    @Override
    @Transactional
    public boolean handleVnpayReturn(Map<String, String> params) {

        log.info("VNPAY return params={}", params);

        String secureHash = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        if (secureHash == null) {
            throw new AppException(ErrorCode.INVALID_SIGNATURE);
        }

        String hashData;
        try {
            hashData = buildHashData(params);
        } catch (Exception e) {
            throw new RuntimeException("Build hash data error", e);
        }

        String calculatedHash;
        try {
            calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        } catch (Exception e) {
            throw new RuntimeException("Hash error", e);
        }

        if (!calculatedHash.equals(secureHash)) {
            throw new AppException(ErrorCode.INVALID_SIGNATURE);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        Payment payment = paymentRepository.findByTransactionId(txnRef)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // chống callback nhiều lần
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment already processed txn={}", txnRef);
            return payment.getStatus() == PaymentStatus.SUCCESS;
        }

        if ("00".equals(responseCode)) {
            handleSuccessPayment(payment);
            return true; // ✅ SUCCESS
        } else {
            handleFailedPayment(payment);
            return false; // ❌ FAIL
        }
    }

    // ================= SUCCESS / FAIL =================

    private void handleSuccessPayment(Payment payment) {

        log.info("Payment SUCCESS txn={}", payment.getTransactionId());

        payment.setStatus(PaymentStatus.SUCCESS);

        Order order = payment.getOrder();

        // 🔥 TRỪ STOCK TẠI ĐÂY
        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            int newStock = product.getStock() - item.getQuantity();

            if (newStock < 0) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            product.setStock(newStock);
        }

        // ✅ UPDATE ORDER
        order.setIsPaid(true);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentTxnId(payment.getTransactionId());

        orderRepository.save(order);
        paymentRepository.save(payment);
    }

    private void handleFailedPayment(Payment payment) {
        log.warn("Payment FAILED txn={}", payment.getTransactionId());

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);
    }

    // ================= GET =================

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        return paymentMapper.toResponse(payment);
    }

    // ================= ADMIN =================

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, String status) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        PaymentStatus newStatus;

        try {
            newStatus = PaymentStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }

        // ❗ không cho update nếu đã SUCCESS
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_STATE);
        }

        payment.setStatus(newStatus);

        if (newStatus == PaymentStatus.SUCCESS) {
            handleSuccessPayment(payment);
        } else {
            paymentRepository.save(payment);
        }

        return paymentMapper.toResponse(payment);
    }

    // ================= VNPAY URL =================

    @Override
    public String generateVnPayUrl(Payment payment, String ipAddress) {

        try {
            Map<String, String> params = new HashMap<>();

            params.put("vnp_Version", vnPayConfig.getVersion());
            params.put("vnp_Command", vnPayConfig.getCommand());
            params.put("vnp_TmnCode", vnPayConfig.getTmnCode());

            // ✅ chuẩn amount
            long amount = payment.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            params.put("vnp_Amount", String.valueOf(amount));

            params.put("vnp_CurrCode", vnPayConfig.getCurrCode());
            params.put("vnp_TxnRef", payment.getTransactionId());
            params.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getId());
            params.put("vnp_OrderType", vnPayConfig.getOrderType());
            params.put("vnp_Locale", vnPayConfig.getLocale());
            params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            params.put("vnp_IpAddr", ipAddress);

            String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            params.put("vnp_CreateDate", createDate);

            String hashData = buildHashData(params);

            String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);

            String query = buildQuery(params);

            return vnPayConfig.getPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;

        } catch (Exception e) {
            throw new RuntimeException("Error generating VNPAY URL", e);
        }
    }

    @Override
    public String getOrderIdFromTxnRef(Map<String, String> params) {
        String txnRef = params.get("vnp_TxnRef");

        if (txnRef == null || !txnRef.startsWith("ORDER_")) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION);
        }

        return txnRef.replace("ORDER_", "");
    }

    // ================= HELPER =================

    private String buildHashData(Map<String, String> params) throws Exception {

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();

        for (String field : fieldNames) {
            String value = params.get(field);

            if (value != null && !value.isEmpty()) {

                hashData.append(field)
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()))
                        .append('&');
            }
        }

        hashData.deleteCharAt(hashData.length() - 1);

        return hashData.toString();
    }

    private String buildQuery(Map<String, String> params) throws Exception {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();

        for (String field : fieldNames) {
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                query.append(URLEncoder.encode(field, StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                        .append('&');
            }
        }

        query.deleteCharAt(query.length() - 1);
        return query.toString();
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 16)
                .toUpperCase();
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA512");
        mac.init(secretKey);
        byte[] raw = mac.doFinal(data.getBytes());

        StringBuilder hex = new StringBuilder();
        for (byte b : raw) {
            String hexByte = Integer.toHexString(0xff & b);
            if (hexByte.length() == 1) hex.append('0');
            hex.append(hexByte);
        }
        return hex.toString();
    }
}