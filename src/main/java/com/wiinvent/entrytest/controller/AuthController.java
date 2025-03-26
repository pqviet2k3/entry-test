package com.wiinvent.entrytest.controller;

import com.wiinvent.entrytest.dto.ApiResponse;
import com.wiinvent.entrytest.dto.JwtResponse;
import com.wiinvent.entrytest.dto.LoginRequest;
import com.wiinvent.entrytest.dto.UserRegistrationRequest;
import com.wiinvent.entrytest.dto.UserResponse;
import com.wiinvent.entrytest.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        UserResponse userResponse = userService.registerUser(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", userResponse));
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtResponse>> authenticateUser(@Valid @RequestBody LoginRequest request) {
        JwtResponse jwtResponse = userService.authenticateUser(request);
        return ResponseEntity.ok(ApiResponse.success("User authenticated successfully", jwtResponse));
    }
} 