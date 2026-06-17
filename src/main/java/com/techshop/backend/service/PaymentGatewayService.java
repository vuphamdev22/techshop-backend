package com.techshop.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // VietQR Configuration
    private static final String VIET_QR_API = "https://api.vietqr.io/v2/generate";
    private static final String ACCOUNT_NO = "0344998620";
    private static final String ACCOUNT_NAME = "PHAM VAN VU";
    private static final String ACQ_ID = "970422"; // MB Bank (Military Bank)

    /**
     * Generate QR code cho thanh toán ngân hàng
     * @param orderId Mã đơn hàng
     * @param amount Số tiền (VNĐ)
     * @return URL QR code
     */
    public String generateQRCode(Long orderId, Double amount) {
        try {
            // Đổi từ USD sang VND (tỷ giá 1 USD = 25.400 VND)
            long amountVnd = Math.round(amount * 25400);

            // Trả về trực tiếp URL ảnh VietQR chuẩn (không lo lỗi tràn cột database VARCHAR(255))
            return String.format(
                "https://img.vietqr.io/image/mb-0344998620-qr_only.png?amount=%d&addInfo=Thanh%%20toan%%20don%%20hang%%20%d&accountName=PHAM%%20VAN%%20VU",
                amountVnd, orderId
            );
        } catch (Exception e) {
            log.error("Lỗi generate QR code: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Lấy thông tin account VietQR
     */
    public static String getAccountName() {
        return ACCOUNT_NAME;
    }

    public static String getAccountNumber() {
        return ACCOUNT_NO;
    }
}
