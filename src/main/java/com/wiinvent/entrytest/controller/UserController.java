package com.wiinvent.entrytest.controller;

import com.wiinvent.entrytest.dto.ApiResponse;
import com.wiinvent.entrytest.dto.UserResponse;
import com.wiinvent.entrytest.dto.DeductPointsRequest;
import com.wiinvent.entrytest.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        UserResponse userResponse = userService.getCurrentUser();
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }
    
    @PostMapping("/points/deduct")
    public ResponseEntity<ApiResponse<UserResponse>> deductPoints(@Valid @RequestBody DeductPointsRequest request) {
        UserResponse userResponse = userService.deductPoints(request);
        return ResponseEntity.ok(ApiResponse.success("Points deducted successfully", userResponse));
    }
} 