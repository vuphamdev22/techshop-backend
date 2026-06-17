package com.techshop.backend.controller;

import com.techshop.backend.dto.response.MembershipResponse;
import com.techshop.backend.dto.response.UserVoucherResponse;
import com.techshop.backend.security.JwtTokenProvider;
import com.techshop.backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/membership")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class MembershipController {

    private final MembershipService membershipService;
    private final JwtTokenProvider jwtTokenProvider;

    /** Get current user's membership status */
    @GetMapping("/me")
    public ResponseEntity<MembershipResponse> getMyMembership(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromHeader(authHeader);
        return ResponseEntity.ok(membershipService.getMembershipStatus(userId));
    }

    /** Get current user's vouchers */
    @GetMapping("/vouchers")
    public ResponseEntity<List<UserVoucherResponse>> getMyVouchers(
            @RequestHeader("Authorization") String authHeader) {
        Long userId = getUserIdFromHeader(authHeader);
        return ResponseEntity.ok(membershipService.getUserVouchers(userId));
    }

    /** Admin: get all user membership summaries */
    @GetMapping("/admin/all")
    public ResponseEntity<List<MembershipResponse>> getAllMemberships(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(membershipService.getAllMembershipSummaries(level, search));
    }

    private Long getUserIdFromHeader(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtTokenProvider.getUserIdFromToken(token);
    }
}
