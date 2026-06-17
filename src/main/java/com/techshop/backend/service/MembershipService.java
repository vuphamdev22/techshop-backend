package com.techshop.backend.service;

import com.techshop.backend.dto.response.MembershipResponse;
import com.techshop.backend.dto.response.UserVoucherResponse;
import java.util.List;

public interface MembershipService {
    /** Called after a successful order to update points/levels */
    void processOrderCompletion(Long userId, Double orderAmount);

    /** Get full membership status for a user */
    MembershipResponse getMembershipStatus(Long userId);

    /** Get user's personal vouchers */
    List<UserVoucherResponse> getUserVouchers(Long userId);

    /** Issue welcome bronze voucher on registration */
    void issueWelcomeVoucher(Long userId);

    /** Admin: get all user membership summaries */
    List<MembershipResponse> getAllMembershipSummaries(String levelFilter, String search);
}
