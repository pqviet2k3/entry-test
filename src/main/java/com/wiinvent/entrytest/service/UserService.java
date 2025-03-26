package com.wiinvent.entrytest.service;

import com.wiinvent.entrytest.dto.JwtResponse;
import com.wiinvent.entrytest.dto.LoginRequest;
import com.wiinvent.entrytest.dto.UserRegistrationRequest;
import com.wiinvent.entrytest.dto.UserResponse;
import com.wiinvent.entrytest.dto.DeductPointsRequest;
import com.wiinvent.entrytest.model.User;

public interface UserService {
    
    UserResponse registerUser(UserRegistrationRequest request);
    
    JwtResponse authenticateUser(LoginRequest request);
    
    UserResponse getCurrentUser();
    
    User getUserById(Long id);
    
    User getUserByUsername(String username);
    
    UserResponse deductPoints(DeductPointsRequest request);
} 