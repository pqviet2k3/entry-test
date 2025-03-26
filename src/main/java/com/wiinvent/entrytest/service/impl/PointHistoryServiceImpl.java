package com.wiinvent.entrytest.service.impl;

import com.wiinvent.entrytest.dto.PointHistoryItem;
import com.wiinvent.entrytest.dto.PointHistoryResponse;
import com.wiinvent.entrytest.model.PointHistory;
import com.wiinvent.entrytest.model.User;
import com.wiinvent.entrytest.repository.PointHistoryRepository;
import com.wiinvent.entrytest.security.UserDetailsImpl;
import com.wiinvent.entrytest.service.PointHistoryService;
import com.wiinvent.entrytest.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointHistoryServiceImpl implements PointHistoryService {
    
    private final PointHistoryRepository pointHistoryRepository;
    private final UserService userService;

    @Override
    public PointHistoryResponse getUserPointHistory(Pageable pageable) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userService.getUserById(userDetails.getId());
        
        Page<PointHistory> pointHistoryPage = pointHistoryRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        
        List<PointHistoryItem> items = pointHistoryPage.getContent().stream()
                .map(this::mapToPointHistoryItem)
                .collect(Collectors.toList());
        
        return PointHistoryResponse.builder()
                .items(items)
                .currentPage(pointHistoryPage.getNumber())
                .totalPages(pointHistoryPage.getTotalPages())
                .totalItems(pointHistoryPage.getTotalElements())
                .build();
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