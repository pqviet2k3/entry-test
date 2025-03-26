package com.wiinvent.entrytest.controller;

import com.wiinvent.entrytest.dto.ApiResponse;
import com.wiinvent.entrytest.dto.CheckinResponse;
import com.wiinvent.entrytest.dto.CheckinStatusResponse;
import com.wiinvent.entrytest.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/checkins")
@RequiredArgsConstructor
public class CheckinController {
    
    private final CheckinService checkinService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<CheckinResponse>> checkin() {
        CheckinResponse response = checkinService.checkin();
        return ResponseEntity.ok(ApiResponse.success("Check-in successful", response));
    }
    
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<CheckinStatusResponse>> getCheckinStatus(
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().withDayOfMonth(1)}") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now()}") 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        CheckinStatusResponse response = checkinService.getCheckinStatus(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
} 