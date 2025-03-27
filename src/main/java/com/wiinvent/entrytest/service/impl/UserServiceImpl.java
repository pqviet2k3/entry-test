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
import com.wiinvent.entrytest.factory.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final PointHistoryRepository pointHistoryRepository;
    private final RedissonClient redissonClient;
    private final RedisKeyFactory redisKeyFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final long CACHE_TTL_HOURS = 24;
    
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
        UserResponse response = mapUserToUserResponse(savedUser);
        
        String cacheKey = redisKeyFactory.generateUserCacheKey(savedUser.getId());
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        return response;
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
        Long userId = userDetails.getId();
        
        String cacheKey = redisKeyFactory.generateUserCacheKey(userId);
        UserResponse cachedUser = (UserResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            return cachedUser;
        }
        
        UserResponse response = mapUserDetailsToUserResponse(userDetails);
        
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        return response;
    }
    
    @Override
    public User getUserById(Long id) {
        String cacheKey = redisKeyFactory.generateUserCacheKey(id);
        User cachedUser = (User) redisTemplate.opsForValue().get(cacheKey);
        if (cachedUser != null) {
            return cachedUser;
        }
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        redisTemplate.opsForValue().set(cacheKey, user, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        return user;
    }
    
    @Override
    public User getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        String cacheKey = redisKeyFactory.generateUserCacheKey(user.getId());
        redisTemplate.opsForValue().set(cacheKey, user, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        return user;
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public UserResponse deductPoints(DeductPointsRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        
        // Distributed lock
        String lockKey = redisKeyFactory.generateLockKey(userId);
        RLock lock = redissonClient.getLock(lockKey);
        
        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new BadRequestException("Another points operation is in progress");
            }
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User not found"));
            
            if (user.getLotusPoints() < request.getPoints()) {
                throw new BadRequestException("Insufficient points");
            }
            
            user.setLotusPoints(user.getLotusPoints() - request.getPoints());
            userRepository.save(user);

            PointHistory pointHistory = PointHistory.builder()
                    .user(user)
                    .points(-request.getPoints())
                    .operationType(OperationType.REDEMPTION)
                    .description(request.getDescription())
                    .build();
            
            pointHistoryRepository.save(pointHistory);
            
            UserResponse response = mapUserToUserResponse(user);
            
            // Update cache
            String cacheKey = redisKeyFactory.generateUserCacheKey(userId);
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
            
            return response;
        } catch (Exception e) {
            // Không cần rollback Redis ở đây vì việc cập nhật cache chỉ xảy ra sau khi giao dịch thành công
            // Nếu có lỗi, transaction sẽ tự rollback và cache sẽ không được cập nhật
            
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new BadRequestException("Points deduction operation was interrupted");
            } else if (e instanceof BadRequestException) {
                throw (BadRequestException) e;
            } else {
                throw new BadRequestException("Failed to deduct points: " + e.getMessage());
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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