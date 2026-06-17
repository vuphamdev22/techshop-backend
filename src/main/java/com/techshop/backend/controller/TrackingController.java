package com.techshop.backend.controller;

import com.techshop.backend.service.EmailMarketingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping("/api/public/track")
@RequiredArgsConstructor
public class TrackingController {

    private final EmailMarketingService emailMarketingService;

    // 1. OPEN TRACKING: Trả về ảnh pixel 1x1 trong suốt
    @GetMapping(value = "/open", produces = MediaType.IMAGE_GIF_VALUE)
    public ResponseEntity<byte[]> trackOpen(@RequestParam("token") String token) {
        try {
            emailMarketingService.trackOpen(token);
        } catch (Exception e) {
            // Silently ignore to avoid breaking mail rendering
        }

        byte[] transparentGif = new byte[]{
                0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00,
                (byte) 0x80, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00, 0x00,
                0x21, (byte) 0xf9, 0x04, 0x01, 0x00, 0x00, 0x00, 0x00, 0x2c, 0x00,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02, 0x02,
                0x44, 0x01, 0x00, 0x3b
        };
        return ResponseEntity.ok(transparentGif);
    }

    // 2. CLICK TRACKING: Chuyển hướng người dùng và ghi nhận click
    @GetMapping("/click")
    public RedirectView trackClick(@RequestParam("token") String token, @RequestParam("redirect") String redirectUrl) {
        try {
            emailMarketingService.trackClick(token);
        } catch (Exception e) {
            // Silently ignore to ensure user gets redirected
        }
        return new RedirectView(redirectUrl);
    }
}
