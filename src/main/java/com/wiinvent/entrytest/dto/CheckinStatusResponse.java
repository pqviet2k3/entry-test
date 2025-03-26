package com.wiinvent.entrytest.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
public class CheckinStatusResponse {
    private Map<LocalDate, DailyStatus> statusMap;
}