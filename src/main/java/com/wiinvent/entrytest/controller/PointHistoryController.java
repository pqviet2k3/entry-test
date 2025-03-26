package com.wiinvent.entrytest.controller;

import com.wiinvent.entrytest.dto.ApiResponse;
import com.wiinvent.entrytest.dto.PointHistoryResponse;
import com.wiinvent.entrytest.service.PointHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/point-history")
@RequiredArgsConstructor
public class PointHistoryController {
    
    private final PointHistoryService pointHistoryService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<PointHistoryResponse>> getPointHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PointHistoryResponse response = pointHistoryService.getUserPointHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
} 