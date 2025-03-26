package com.wiinvent.entrytest.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeductPointsRequest {
    
    @Min(value = 1, message = "Points must be greater than 0")
    private Integer points;
    
    @NotBlank(message = "Description is required")
    private String description;
} 