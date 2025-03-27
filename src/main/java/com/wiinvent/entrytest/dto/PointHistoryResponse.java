package com.wiinvent.entrytest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryResponse {
    
    private List<PointHistoryItem> items;
    private int currentPage;
    private int totalPages;
    private long totalItems;
} 