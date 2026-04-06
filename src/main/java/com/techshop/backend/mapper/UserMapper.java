package com.techshop.backend.mapper;

import com.techshop.backend.dto.response.admin.AdminUserResponse;
import com.techshop.backend.entity.User;

public class UserMapper {

    public static AdminUserResponse toAdminResponse(User user, int orderCount, double totalSpent) {
        AdminUserResponse res = new AdminUserResponse();
        res.setId(user.getId());
        res.setFirstName(user.getFirstName());
        res.setLastName(user.getLastName());
        res.setEmail(user.getEmail());
        res.setPhone(user.getPhone());
        res.setAddress(user.getAddress());
        res.setRole(user.getRole());
        res.setEnabled(user.isEnabled());
        res.setCreatedAt(user.getCreatedAt());
        res.setOrderCount(orderCount);
        res.setTotalSpent(totalSpent);
        return res;
    }
}