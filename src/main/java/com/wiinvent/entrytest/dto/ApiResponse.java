package com.wiinvent.entrytest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    
    public static <T> ApiResponse<T> success(T data) {
        return (ApiResponse<T>) ApiResponse.builder()
                .success(true)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> success(String message, T data) {
        return (ApiResponse<T>) ApiResponse.builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return (ApiResponse<T>) ApiResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    public void setData(T data) {
        this.data = data;
    }
} 