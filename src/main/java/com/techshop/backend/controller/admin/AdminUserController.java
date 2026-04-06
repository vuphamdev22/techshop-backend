package com.techshop.backend.controller.admin;

import com.techshop.backend.dto.request.AdminUserRequest;
import com.techshop.backend.dto.response.admin.AdminUserResponse;
import com.techshop.backend.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public List<AdminUserResponse> getAllUsers() {
        return service.getAllUsers();
    }

    @GetMapping("/{id}")
    public AdminUserResponse getUserById(@PathVariable Long id) {
        return service.getUserById(id);
    }

    @PutMapping("/{id}")
    public AdminUserResponse updateUser(@PathVariable Long id, @RequestBody AdminUserRequest request) {
        return service.updateUser(id, request);
    }

    @PutMapping("/{id}/status")
    public AdminUserResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return service.setUserEnabled(id, enabled);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        service.deleteUser(id);
    }
}
