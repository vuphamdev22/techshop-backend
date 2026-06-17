package com.techshop.backend.service.Impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.techshop.backend.config.VietQRConfig;
import com.techshop.backend.dto.request.VietQRGenerateRequest;
import com.techshop.backend.dto.response.VietQRResponse;
import com.techshop.backend.entity.Payment;
import com.techshop.backend.service.VietQRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VietQRServiceImpl implements VietQRService {

    private final VietQRConfig vietQRConfig;

    @Override
    public VietQRResponse generateVietQRCode(Payment payment) {
        log.info("Generating VietQR code for payment: {}", payment.getId());

        String description = String.format("Thanh toan don hang %d", payment.getOrder().getId());
        return generateVietQRCodeInternal(payment.getAmount(), description, payment.getTransactionId());
    }

    @Override
    public VietQRResponse generateVietQRCode(com.techshop.backend.dto.request.VietQRGenerateRequest request) {
        log.info("Generating VietQR code for raw request with amount: {}", request.getAmount());
        String qrTransactionId = "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        return generateVietQRCodeInternal(request.getAmount(), request.getDescription(), qrTransactionId);
    }

    private VietQRResponse generateVietQRCodeInternal(Double amount, String description, String transactionId) {
        try {
            long amountInVND = Math.round(amount);

            // Tạo VietQR data content
            String qrContent = String.format(
                "00020126360014COM.VIETQR01051200%s0208%s52040000530376541610%d63041E99",
                String.format("%020d", Long.valueOf(vietQRConfig.getAccountNumber())),
                String.format("%02d", vietQRConfig.getBankCode().length()) + vietQRConfig.getBankCode(),
                amountInVND
            );

            String qrImage = generateQRCodeImage(qrContent);

            VietQRResponse response = new VietQRResponse();
            response.setAccountNumber(vietQRConfig.getAccountNumber());
            response.setAccountName(vietQRConfig.getAccountName());
            response.setBankCode(vietQRConfig.getBankCode());
            response.setBankName(vietQRConfig.getBankName());
            response.setAmount(amount);
            response.setDescription(description);
            response.setQrCodeImage(qrImage);
            response.setTransactionId(transactionId);

            log.info("VietQR code generated successfully");
            return response;
        } catch (Exception e) {
            log.error("Error generating VietQR code: ", e);
            throw new RuntimeException("Failed to generate VietQR code", e);
        }
    }

    @Override
    public String generateQRCodeImage(String qrContent) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            int size = vietQRConfig.getQrCodeSize() != null ? vietQRConfig.getQrCodeSize() : 300;
            BitMatrix bitMatrix = writer.encode(qrContent, BarcodeFormat.QR_CODE, size, size);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "PNG", baos);
            byte[] imageData = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageData);

            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            log.error("Error generating QR code image: ", e);
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }

    @Override
    public boolean verifyPaymentStatus(String transactionId) {
        // TODO: Integrate with VietQR API to verify payment status
        // This would call VietQR's API or partner bank's API to check if payment was made
        log.info("Verifying payment status for transaction: {}", transactionId);
        return false;
    }
}
