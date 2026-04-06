package com.techshop.backend.service;

import com.techshop.backend.dto.request.AdminUserRequest;
import com.techshop.backend.dto.response.admin.AdminUserResponse;

import java.util.List;

public interface AdminUserService {
    List<AdminUserResponse> getAllUsers();
    AdminUserResponse getUserById(Long id);
    AdminUserResponse updateUser(Long id, AdminUserRequest request);
    void deleteUser(Long id);
    AdminUserResponse setUserEnabled(Long id, boolean enabled);
}
