package com.wiinvent.entrytest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DailyStatus {
    private LocalDate date;
    private boolean checkedIn;
    private int pointsEarned;
}
