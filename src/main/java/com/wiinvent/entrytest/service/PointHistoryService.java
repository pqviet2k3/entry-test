package com.wiinvent.entrytest.service;

import com.wiinvent.entrytest.dto.PointHistoryResponse;
import org.springframework.data.domain.Pageable;

public interface PointHistoryService {
    
    PointHistoryResponse getUserPointHistory(Pageable pageable);
} 