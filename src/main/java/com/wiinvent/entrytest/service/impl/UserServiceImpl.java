package com.wiinvent.entrytest.service.impl;

import com.wiinvent.entrytest.dto.JwtResponse;
import com.wiinvent.entrytest.dto.LoginRequest;
import com.wiinvent.entrytest.dto.UserRegistrationRequest;
import com.wiinvent.entrytest.dto.UserResponse;
import com.wiinvent.entrytest.dto.DeductPointsRequest;
import com.wiinvent.entrytest.exception.BadRequestException;
import com.wiinvent.entrytest.exception.ResourceNotFoundException;
import com.wiinvent.entrytest.model.User;
import com.wiinvent.entrytest.model.PointHistory;
import com.wiinvent.entrytest.repository.UserRepository;
import com.wiinvent.entrytest.repository.PointHistoryRepository;
import com.wiinvent.entrytest.security.JwtUtils;
import com.wiinvent.entrytest.security.UserDetailsImpl;
import com.wiinvent.entrytest.service.UserService;
import com.wiinvent.entrytest.enumeration.OperationType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PointHistoryRepository pointHistoryRepository;
    
    @Override
    @Transactional
    public UserResponse registerUser(UserRegistrationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }
        
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .lotusPoints(0)
                .build();
        
        User savedUser = userRepository.save(user);
        
        return mapUserToUserResponse(savedUser);
    }
    
    @Override
    public JwtResponse authenticateUser(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(authentication);
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        return new JwtResponse(jwt, mapUserDetailsToUserResponse(userDetails));
    }
    
    @Override
    public UserResponse getCurrentUser() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return mapUserDetailsToUserResponse(userDetails);
    }
    
    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
    
    @Override
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }
    
    @Override
    @Transactional
    public UserResponse deductPoints(DeductPointsRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));
        
        if (user.getLotusPoints() < request.getPoints()) {
            throw new BadRequestException("Insufficient points");
        }
        user.setLotusPoints(user.getLotusPoints() - request.getPoints());
        userRepository.save(user);

        // Create point history
        PointHistory pointHistory = PointHistory.builder()
                .user(user)
                .points(-request.getPoints())
                .operationType(OperationType.REDEMPTION)
                .description(request.getDescription())
                .build();
        
        pointHistoryRepository.save(pointHistory);
        
        return mapUserToUserResponse(user);
    }
    
    private UserResponse mapUserToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .lotusPoints(user.getLotusPoints())
                .createdAt(user.getCreatedAt())
                .build();
    }
    
    private UserResponse mapUserDetailsToUserResponse(UserDetailsImpl userDetails) {
        return UserResponse.builder()
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .fullName(userDetails.getFullName())
                .lotusPoints(userDetails.getLotusPoints())
                .build();
    }
} 