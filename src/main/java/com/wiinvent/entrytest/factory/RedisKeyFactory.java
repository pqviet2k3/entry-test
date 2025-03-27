package com.wiinvent.entrytest.factory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class RedisKeyFactory {
    @Value("${app.redis.prefix}")
    private String REDIS_PREFIX;
    
    public String generateLockKey(Long userId) {
        return REDIS_PREFIX + ":" + "lock" + ":" + userId + ":" + UUID.randomUUID();
    }
    
    public String generateCheckinKey(Long userId, LocalDate date) {
        return REDIS_PREFIX + ":" + "checkin" + ":" + userId + ":" + date;
    }
    
    public String generateMonthlyCheckinKey(Long userId, LocalDate date) {
        return REDIS_PREFIX + ":" + "monthly" + ":" + userId + ":" + date.getYear() + ":" + date.getMonthValue();
    }

    public String generateUserCacheKey(Long userId) {
        return REDIS_PREFIX + ":" + "cache" + ":" + "user" + ":" + userId;
    }

    public String generatePointHistoryCacheKey(Long userId, int page, int size) {
        return REDIS_PREFIX + ":" + "cache" + ":" + "point_history" + ":" + userId + ":" + page + ":" + size;
    }

    public String generateCheckinStatusCacheKey(Long userId, LocalDate startDate, LocalDate endDate) {
        return REDIS_PREFIX + ":" + "cache" + ":" + "checkin_status" + ":" + userId + ":" + startDate + ":" + endDate;
    }
} 