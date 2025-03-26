package com.wiinvent.entrytest.dto;

import com.wiinvent.entrytest.enumeration.OperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointHistoryItem {
    private Long id;
    private Integer points;
    private OperationType operationType;
    private String description;
    private LocalDateTime createdAt;
}