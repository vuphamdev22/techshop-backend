package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.response.MembershipResponse;
import com.techshop.backend.dto.response.UserVoucherResponse;
import com.techshop.backend.entity.Coupon;
import com.techshop.backend.entity.MembershipHistory;
import com.techshop.backend.entity.User;
import com.techshop.backend.entity.UserVoucher;
import com.techshop.backend.enums.MembershipLevel;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.repository.CouponRepository;
import com.techshop.backend.repository.MembershipHistoryRepository;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.repository.UserVoucherRepository;
import com.techshop.backend.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MembershipServiceImpl implements MembershipService {

    // Thresholds
    private static final int SILVER_ORDERS = 3;
    private static final double SILVER_SPENT = 300.0;   // $300 equivalent
    private static final int GOLD_ORDERS = 10;
    private static final double GOLD_SPENT = 1000.0;    // $1000 equivalent

    // Points: 1 point per $10 spent (Gold = x2)
    private static final double POINTS_PER_DOLLAR = 0.1;

    // EXP: 10 exp per $1 spent
    private static final double EXP_PER_DOLLAR = 10.0;

    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final UserVoucherRepository userVoucherRepository;
    private final MembershipHistoryRepository membershipHistoryRepository;

    @Override
    @Transactional
    public void processOrderCompletion(Long userId, Double orderAmount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        double amount = orderAmount != null ? orderAmount : 0.0;

        // 1. Update totals
        double currentSpent = user.getTotalSpent() != null ? user.getTotalSpent() : 0.0;
        int currentOrders = user.getTotalOrders() != null ? user.getTotalOrders() : 0;
        user.setTotalSpent(currentSpent + amount);
        user.setTotalOrders(currentOrders + 1);

        // 2. Calculate points (Gold = x2)
        MembershipLevel currentLevel = user.getMembershipLevel() != null ? user.getMembershipLevel() : MembershipLevel.BRONZE;
        int pointsEarned;
        if (currentLevel == MembershipLevel.GOLD) {
            pointsEarned = (int) (amount * POINTS_PER_DOLLAR * 2);
        } else {
            pointsEarned = (int) (amount * POINTS_PER_DOLLAR);
        }
        int currentPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        user.setRewardPoints(currentPoints + pointsEarned);

        // 3. EXP
        int currentExp = user.getExp() != null ? user.getExp() : 0;
        int expEarned = (int) (amount * EXP_PER_DOLLAR);
        user.setExp(currentExp + expEarned);

        // 4. Check upgrade
        MembershipLevel oldLevel = user.getMembershipLevel() != null ? user.getMembershipLevel() : MembershipLevel.BRONZE;
        MembershipLevel newLevel = calculateLevel(user);

        if (newLevel != oldLevel) {
            user.setMembershipLevel(newLevel);

            // Record history
            MembershipHistory history = MembershipHistory.builder()
                    .user(user)
                    .fromLevel(oldLevel)
                    .toLevel(newLevel)
                    .note("Auto-upgraded after order completion. Orders: " + user.getTotalOrders() + ", Spent: $" + user.getTotalSpent())
                    .upgradedAt(LocalDateTime.now())
                    .build();
            membershipHistoryRepository.save(history);

            // Generate upgrade voucher
            generateUpgradeVoucher(user, newLevel);

            log.info("User {} upgraded from {} to {}", userId, oldLevel, newLevel);
        }

        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipResponse getMembershipStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return buildMembershipResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserVoucherResponse> getUserVouchers(Long userId) {
        return userVoucherRepository.findByUserIdAndUsedFalse(userId).stream()
                .map(uv -> UserVoucherResponse.builder()
                        .id(uv.getId())
                        .code(uv.getCoupon().getCode())
                        .discountType(uv.getCoupon().getDiscountType())
                        .discountValue(uv.getCoupon().getDiscountValue())
                        .minOrderValue(uv.getCoupon().getMinOrderValue())
                        .endDate(uv.getCoupon().getEndDate())
                        .used(uv.isUsed())
                        .assignedAt(uv.getAssignedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void issueWelcomeVoucher(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Coupon coupon = createCoupon(
                "WELCOME-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                "percentage",
                5.0,
                0.0,
                LocalDateTime.now().plusDays(30),
                "Voucher chào mừng thành viên mới (Bronze 5%)"
        );

        UserVoucher uv = UserVoucher.builder()
                .user(user)
                .coupon(coupon)
                .assignedAt(LocalDateTime.now())
                .build();
        userVoucherRepository.save(uv);

        log.info("Welcome voucher issued to user {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipResponse> getAllMembershipSummaries(String levelFilter, String search) {
        List<User> users = userRepository.findAll();

        return users.stream()
                .filter(u -> {
                    MembershipLevel lvl = u.getMembershipLevel() != null ? u.getMembershipLevel() : MembershipLevel.BRONZE;
                    boolean matchLevel = levelFilter == null || levelFilter.isBlank() ||
                            lvl.name().equalsIgnoreCase(levelFilter);
                    boolean matchSearch = search == null || search.isBlank() ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(search.toLowerCase())) ||
                            (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(search.toLowerCase())) ||
                            (u.getLastName() != null && u.getLastName().toLowerCase().contains(search.toLowerCase()));
                    return matchLevel && matchSearch;
                })
                .map(this::buildMembershipResponse)
                .collect(Collectors.toList());
    }

    // ========== Private helpers ==========

    private MembershipLevel calculateLevel(User user) {
        int orders = user.getTotalOrders() != null ? user.getTotalOrders() : 0;
        double spent = user.getTotalSpent() != null ? user.getTotalSpent() : 0.0;

        if (orders >= GOLD_ORDERS || spent >= GOLD_SPENT) {
            return MembershipLevel.GOLD;
        } else if (orders >= SILVER_ORDERS || spent >= SILVER_SPENT) {
            return MembershipLevel.SILVER;
        }
        return MembershipLevel.BRONZE;
    }

    private void generateUpgradeVoucher(User user, MembershipLevel level) {
        String code;
        double discountPct;
        String desc;

        switch (level) {
            case SILVER -> {
                code = "SILVER-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                discountPct = 10.0;
                desc = "Voucher thăng hạng Silver (10% off)";
            }
            case GOLD -> {
                code = "GOLD-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                discountPct = 15.0;
                desc = "Voucher thăng hạng Gold (15% off)";
            }
            default -> {
                return; // No voucher for BRONZE
            }
        }

        Coupon coupon = createCoupon(code, "percentage", discountPct, 0.0,
                LocalDateTime.now().plusDays(60), desc);

        UserVoucher uv = UserVoucher.builder()
                .user(user)
                .coupon(coupon)
                .assignedAt(LocalDateTime.now())
                .build();
        userVoucherRepository.save(uv);
    }

    private Coupon createCoupon(String code, String type, double value, double minOrder,
                                LocalDateTime endDate, String description) {
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setDiscountType(type);
        coupon.setDiscountValue(value);
        coupon.setMinOrderValue(minOrder);
        coupon.setStartDate(LocalDateTime.now());
        coupon.setEndDate(endDate);
        coupon.setUsageLimit(1);
        coupon.setUsedCount(0);
        coupon.setIsActive(true);
        return couponRepository.save(coupon);
    }

    private MembershipResponse buildMembershipResponse(User user) {
        MembershipLevel level = user.getMembershipLevel();
        if (level == null) {
            level = MembershipLevel.BRONZE;
        }
        int totalOrders = user.getTotalOrders() != null ? user.getTotalOrders() : 0;
        double totalSpent = user.getTotalSpent() != null ? user.getTotalSpent() : 0.0;
        int rewardPoints = user.getRewardPoints() != null ? user.getRewardPoints() : 0;
        int exp = user.getExp() != null ? user.getExp() : 0;

        MembershipLevel nextLevel = null;
        Integer ordersToNext = null;
        Double spentToNext = null;
        double progressPercent = 100.0;
        String progressLabel = "Bạn đang ở hạng cao nhất 🏆";

        if (level == MembershipLevel.BRONZE) {
            nextLevel = MembershipLevel.SILVER;
            int ordersDiff = Math.max(0, SILVER_ORDERS - totalOrders);
            double spentDiff = Math.max(0, SILVER_SPENT - totalSpent);
            ordersToNext = ordersDiff;
            spentToNext = spentDiff;
            // Progress by orders metric
            progressPercent = Math.min(100.0, (totalOrders * 100.0) / SILVER_ORDERS);
            progressLabel = ordersDiff > 0
                    ? "Còn " + ordersDiff + " đơn nữa để lên Silver 🥈"
                    : "Chi thêm $" + String.format("%.0f", spentDiff) + " để lên Silver 🥈";

        } else if (level == MembershipLevel.SILVER) {
            nextLevel = MembershipLevel.GOLD;
            int ordersDiff = Math.max(0, GOLD_ORDERS - totalOrders);
            double spentDiff = Math.max(0, GOLD_SPENT - totalSpent);
            ordersToNext = ordersDiff;
            spentToNext = spentDiff;
            progressPercent = Math.min(100.0, (totalOrders * 100.0) / GOLD_ORDERS);
            progressLabel = ordersDiff > 0
                    ? "Còn " + ordersDiff + " đơn nữa để lên Gold 🥇"
                    : "Chi thêm $" + String.format("%.0f", spentDiff) + " để lên Gold 🥇";
        }

        return MembershipResponse.builder()
                .userId(user.getId())
                .fullName((user.getFirstName() != null ? user.getFirstName() : "") + " " + (user.getLastName() != null ? user.getLastName() : ""))
                .email(user.getEmail())
                .membershipLevel(level)
                .rewardPoints(rewardPoints)
                .totalSpent(totalSpent)
                .totalOrders(totalOrders)
                .exp(exp)
                .nextLevel(nextLevel)
                .ordersToNextLevel(ordersToNext)
                .spentToNextLevel(spentToNext)
                .progressPercent(progressPercent)
                .progressLabel(progressLabel)
                .build();
    }
}
