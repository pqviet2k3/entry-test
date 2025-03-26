package com.wiinvent.entrytest.service;

import com.wiinvent.entrytest.dto.CheckinResponse;
import com.wiinvent.entrytest.dto.CheckinStatusResponse;

import java.time.LocalDate;

public interface CheckinService {
    
    CheckinResponse checkin();
    
    CheckinStatusResponse getCheckinStatus(LocalDate startDate, LocalDate endDate);
} 