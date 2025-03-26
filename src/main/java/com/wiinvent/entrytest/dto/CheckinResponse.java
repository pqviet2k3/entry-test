package com.wiinvent.entrytest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckinResponse {
    
    private Long id;
    private LocalDate checkinDate;
    private LocalTime checkinTime;
    private Integer pointsEarned;
    private String message;
    private boolean success;
    private int totalPoints;
} 