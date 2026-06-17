package com.techshop.backend.service.Impl;

import com.techshop.backend.config.VnPayConfig;
import com.techshop.backend.dto.request.PaymentCreateRequest;
import com.techshop.backend.dto.response.PaymentResponse;
import com.techshop.backend.dto.response.VietQRResponse;
import com.techshop.backend.entity.Order;
import com.techshop.backend.entity.Payment;
import com.techshop.backend.enums.OrderStatus;
import com.techshop.backend.enums.PaymentMethod;
import com.techshop.backend.enums.PaymentStatus;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.PaymentMapper;
import com.techshop.backend.repository.OrderRepository;
import com.techshop.backend.repository.PaymentRepository;
import com.techshop.backend.service.PaymentService;
import com.techshop.backend.service.VietQRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
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
    private final VietQRService vietQRService;

    @Override
    @Transactional
    public PaymentResponse createPayment(Long orderId, PaymentCreateRequest request) {
        log.info("Creating payment for order: {}", orderId);

        // 1. Kiểm tra order tồn tại
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        // 2. Kiểm tra order chưa có payment
        if (paymentRepository.findByOrderId(orderId).isPresent()) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_EXISTS);
        }

        // 3. Kiểm tra payment method hợp lệ cho online
        if (request.getMethod() == PaymentMethod.COD) {
            throw new AppException(ErrorCode.INVALID_PAYMENT_METHOD);
        }

        // 4. Tạo payment
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.getMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAmount(order.getTotalPrice());
        payment.setTransactionId(generateTransactionId());

        Payment savedPayment = paymentRepository.save(payment);

        // 5. Generate payment URL (mock)
        String paymentUrl = generateMockPaymentUrl(savedPayment);

        log.info("Payment created successfully: {}", savedPayment.getId());
        return paymentMapper.toResponseWithUrl(savedPayment, paymentUrl);
    }

    @Override
    @Transactional
    public void handlePaymentCallback(String transactionId, boolean success, String note) {
        log.info("Handling payment callback for transaction: {}", transactionId);

        // 1. Tìm payment theo transactionId
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        // 2. Cập nhật trạng thái payment
        if (success) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setNote(note != null ? note : "Payment successful");

            // 3. Cập nhật order
            Order order = payment.getOrder();
            order.setIsPaid(true);
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentTxnId(transactionId);

            orderRepository.save(order);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setNote(note != null ? note : "Payment failed");

            // Có thể cancel order hoặc để pending
            // Ở đây để pending, admin có thể xử lý
        }

        paymentRepository.save(payment);
        log.info("Payment callback handled: {}", success ? "SUCCESS" : "FAILED");
    }

    @Override
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional
    public PaymentResponse updatePaymentStatus(Long paymentId, String status) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        try {
            PaymentStatus paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            payment.setStatus(paymentStatus);

            // Nếu success, cập nhật order
            if (paymentStatus == PaymentStatus.SUCCESS) {
                Order order = payment.getOrder();
                order.setIsPaid(true);
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }

            Payment saved = paymentRepository.save(payment);
            return paymentMapper.toResponse(saved);

        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS);
        }
    }

    @Override
    public String generateMockPaymentUrl(Payment payment) {
        if (payment.getMethod() == PaymentMethod.VNPAY) {
            return generateVnPayUrl(payment);
        } else if (payment.getMethod() == PaymentMethod.VIETQR) {
            // VietQR không cần URL, chỉ cần QR code
            return "VIETQR_QR_CODE_GENERATED";
        }
        // Mock URL cho development
        // Trong production, tích hợp với VNPAY/MOMO
        return String.format("https://mock-payment-gateway.com/pay?txn=%s&amount=%.2f&method=%s",
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getMethod());
    }

    private String generateTransactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateVnPayUrl(Payment payment) {
        try {
            Map<String, String> vnpParams = new HashMap<>();

            // Thông tin cơ bản
            vnpParams.put("vnp_Version", vnPayConfig.getVersion());
            vnpParams.put("vnp_Command", vnPayConfig.getCommand());
            vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
            vnpParams.put("vnp_Amount", String.valueOf((long)(payment.getAmount() * 100))); // VNPay yêu cầu amount * 100
            vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());
            vnpParams.put("vnp_TxnRef", payment.getTransactionId());
            vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + payment.getOrder().getId());
            vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
            vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
            vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
            vnpParams.put("vnp_IpAddr", "127.0.0.1"); // Có thể lấy từ request

            // Thời gian tạo
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String vnpCreateDate = formatter.format(new Date());
            vnpParams.put("vnp_CreateDate", vnpCreateDate);

            // Thời gian hết hạn (15 phút)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 15);
            String vnpExpireDate = formatter.format(calendar.getTime());
            vnpParams.put("vnp_ExpireDate", vnpExpireDate);

            // Sắp xếp tham số theo thứ tự alphabet
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Tạo chuỗi hash
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            for (String fieldName : fieldNames) {
                String fieldValue = vnpParams.get(fieldName);
                if (fieldValue != null && fieldValue.length() > 0) {
                    hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())).append('&');
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString())).append('=')
                         .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString())).append('&');
                }
            }

            // Xóa ký tự '&' cuối cùng
            hashData.setLength(hashData.length() - 1);
            query.setLength(query.length() - 1);

            // Tạo chữ ký HMAC-SHA512
            String vnpSecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());

            // Thêm chữ ký vào query string
            query.append("&vnp_SecureHash=").append(vnpSecureHash);

            return vnPayConfig.getPayUrl() + "?" + query.toString();

        } catch (Exception e) {
            log.error("Error generating VNPay URL", e);
            throw new AppException(ErrorCode.PAYMENT_CREATION_FAILED);
        }
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmacSha512 = Mac.getInstance("HmacSHA512");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        hmacSha512.init(secretKey);
        byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @Override
    public void handleVnPayCallback(Map<String, String> vnpParams) {
        try {
            // Lấy thông tin từ VNPay
            String vnpSecureHash = vnpParams.remove("vnp_SecureHash");
            String transactionId = vnpParams.get("vnp_TxnRef");
            String responseCode = vnpParams.get("vnp_ResponseCode");
            String amount = vnpParams.get("vnp_Amount");

            log.info("Processing VNPay callback - Transaction: {}, Response Code: {}, Amount: {}",
                    transactionId, responseCode, amount);

            // Validate chữ ký (nếu cần thiết)
            // String expectedHash = calculateHash(vnpParams);
            // if (!expectedHash.equals(vnpSecureHash)) {
            //     log.error("Invalid VNPay signature");
            //     return;
            // }

            // Kiểm tra response code (00 = thành công)
            boolean success = "00".equals(responseCode);

            String note = success ? "Payment successful via VNPay" :
                          "Payment failed via VNPay: " + responseCode;

            // Xử lý callback
            handlePaymentCallback(transactionId, success, note);

        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            throw new AppException(ErrorCode.PAYMENT_PROCESSING_FAILED);
        }
    }
}