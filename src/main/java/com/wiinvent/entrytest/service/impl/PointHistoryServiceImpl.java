package com.wiinvent.entrytest.service.impl;

import com.wiinvent.entrytest.dto.PointHistoryItem;
import com.wiinvent.entrytest.dto.PointHistoryResponse;
import com.wiinvent.entrytest.model.PointHistory;
import com.wiinvent.entrytest.model.User;
import com.wiinvent.entrytest.repository.PointHistoryRepository;
import com.wiinvent.entrytest.security.UserDetailsImpl;
import com.wiinvent.entrytest.service.PointHistoryService;
import com.wiinvent.entrytest.service.UserService;
import com.wiinvent.entrytest.factory.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointHistoryServiceImpl implements PointHistoryService {
    
    private final PointHistoryRepository pointHistoryRepository;
    private final UserService userService;
    private final RedisKeyFactory redisKeyFactory;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final long CACHE_TTL_HOURS = 24;

    @Override
    public PointHistoryResponse getUserPointHistory(Pageable pageable) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        
        String cacheKey = redisKeyFactory.generatePointHistoryCacheKey(userId, pageable.getPageNumber(), pageable.getPageSize());
        PointHistoryResponse cachedResponse = (PointHistoryResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        
        User user = userService.getUserById(userId);
        Page<PointHistory> pointHistoryPage = pointHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        List<PointHistoryItem> items = pointHistoryPage.getContent().stream()
                .map(this::mapToPointHistoryItem)
                .collect(Collectors.toList());
        
        PointHistoryResponse response = PointHistoryResponse.builder()
                .items(items)
                .currentPage(pointHistoryPage.getNumber())
                .totalPages(pointHistoryPage.getTotalPages())
                .totalItems(pointHistoryPage.getTotalElements())
                .build();
        
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        return response;
    }
    
    private PointHistoryItem mapToPointHistoryItem(PointHistory pointHistory) {
        return PointHistoryItem.builder()
                .id(pointHistory.getId())
                .points(pointHistory.getPoints())
                .operationType(pointHistory.getOperationType())
                .description(pointHistory.getDescription())
                .createdAt(pointHistory.getCreatedAt())
                .build();
    }
} 