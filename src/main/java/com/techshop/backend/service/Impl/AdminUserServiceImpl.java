package com.techshop.backend.service.Impl;

import com.techshop.backend.dto.request.AdminUserRequest;
import com.techshop.backend.dto.response.admin.AdminUserResponse;
import com.techshop.backend.entity.Order;
import com.techshop.backend.entity.User;
import com.techshop.backend.exception.AppException;
import com.techshop.backend.exception.ErrorCode;
import com.techshop.backend.mapper.UserMapper;
import com.techshop.backend.repository.OrderRepository;
import com.techshop.backend.repository.UserRepository;
import com.techshop.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<AdminUserResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream().map(this::buildAdminUserResponse).toList();
    }

    @Override
    public AdminUserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return buildAdminUserResponse(user);
    }

    @Override
    public AdminUserResponse updateUser(Long id, AdminUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());

        User saved = userRepository.save(user);
        return buildAdminUserResponse(saved);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userRepository.delete(user);
    }

    @Override
    public AdminUserResponse setUserEnabled(Long id, boolean enabled) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        user.setEnabled(enabled);
        User saved = userRepository.save(user);
        return buildAdminUserResponse(saved);
    }

    private AdminUserResponse buildAdminUserResponse(User user) {
        List<Order> orders = orderRepository.findByUserId(user.getId());
        int orderCount = orders.size();
        double totalSpent = orders.stream().mapToDouble(o -> o.getTotalPrice() == null ? 0.0 : o.getTotalPrice()).sum();
        return UserMapper.toAdminResponse(user, orderCount, totalSpent);
    }
}
