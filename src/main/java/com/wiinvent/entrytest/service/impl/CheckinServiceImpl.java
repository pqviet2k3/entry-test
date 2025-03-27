package com.wiinvent.entrytest.service.impl;

import com.wiinvent.entrytest.dto.CheckinResponse;
import com.wiinvent.entrytest.dto.CheckinStatusResponse;
import com.wiinvent.entrytest.dto.DailyStatus;
import com.wiinvent.entrytest.exception.BadRequestException;
import com.wiinvent.entrytest.model.Checkin;
import com.wiinvent.entrytest.model.PointHistory;
import com.wiinvent.entrytest.model.User;
import com.wiinvent.entrytest.repository.CheckinRepository;
import com.wiinvent.entrytest.repository.PointHistoryRepository;
import com.wiinvent.entrytest.repository.UserRepository;
import com.wiinvent.entrytest.security.UserDetailsImpl;
import com.wiinvent.entrytest.service.CheckinService;
import com.wiinvent.entrytest.enumeration.OperationType;
import com.wiinvent.entrytest.enumeration.CheckinPoints;
import com.wiinvent.entrytest.factory.RedisKeyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckinServiceImpl implements CheckinService {
    
    private final CheckinRepository checkinRepository;
    private final UserRepository userRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisKeyFactory redisKeyFactory;
    
    @Value("${game.checkin.morning-start}")
    private String morningStart;
    
    @Value("${game.checkin.morning-end}")
    private String morningEnd;
    
    @Value("${game.checkin.evening-start}")
    private String eveningStart;
    
    @Value("${game.checkin.evening-end}")
    private String eveningEnd;
    
    @Value("${game.checkin.max-per-month}")
    private int maxCheckinsPerMonth;
    
    private static final long CACHE_TTL_HOURS = 24;
    
    private LocalTime getMorningStart() {
        return LocalTime.parse(morningStart);
    }
    
    private LocalTime getMorningEnd() {
        return LocalTime.parse(morningEnd);
    }
    
    private LocalTime getEveningStart() {
        return LocalTime.parse(eveningStart);
    }
    
    private LocalTime getEveningEnd() {
        return LocalTime.parse(eveningEnd);
    }
    
    private boolean isTimeInAllowedRanges(LocalTime time) {
        return (time.equals(getMorningStart()) || time.isAfter(getMorningStart()) && time.isBefore(getMorningEnd())) ||
               (time.equals(getEveningStart()) || time.isAfter(getEveningStart()) && time.isBefore(getEveningEnd()));
    }
    
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public CheckinResponse checkin() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        
        // isChecked in today
        String checkinKey = redisKeyFactory.generateCheckinKey(userId, today);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(checkinKey))) {
            throw new BadRequestException("You have already checked in today");
        }
        
        // Distributed lock
        String lockKey = redisKeyFactory.generateLockKey(userId);
        RLock lock = redissonClient.getLock(lockKey);
        
        String monthlyKey = redisKeyFactory.generateMonthlyCheckinKey(userId, today);
        Long originalMonthlyCheckins = null;
        boolean needRollback = false;
        
        CheckinResponse response;
        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new BadRequestException("Another check-in operation is in progress");
            }
            
            if (!isTimeInAllowedRanges(now)) {
                throw new BadRequestException("Check-in is only allowed during morning (9:00-11:00) or evening (19:00-21:00) hours");
            }
            
            // initial value
            Object existingValue = redisTemplate.opsForValue().get(monthlyKey);
            originalMonthlyCheckins = existingValue != null ? (Long) existingValue : 0L;
            
            // Check monthly checkins
            Long monthlyCheckins = redisTemplate.opsForValue().increment(monthlyKey);
            needRollback = true;
            
            if (monthlyCheckins == null) {
                LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
                long daysUntilEndOfMonth = ChronoUnit.DAYS.between(today, endOfMonth) + 1;
                redisTemplate.opsForValue().set(monthlyKey, 1L, daysUntilEndOfMonth, TimeUnit.DAYS);
            } else if (monthlyCheckins > maxCheckinsPerMonth) {
                // Rollback Redis
                redisTemplate.opsForValue().set(monthlyKey, originalMonthlyCheckins);
                throw new BadRequestException("You have reached the maximum number of check-ins for this month");
            }
            
            int points = CheckinPoints.getPointsForDay(today.getDayOfWeek().getValue());
            
            Checkin checkin = Checkin.builder()
                    .user(userRepository.findById(userId)
                            .orElseThrow(() -> new BadRequestException("User not found")))
                    .checkinDate(today)
                    .checkinTime(now)
                    .pointsEarned(points)
                    .build();
            
            checkinRepository.save(checkin);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BadRequestException("User not found"));
            user.setLotusPoints(user.getLotusPoints() + points);
            userRepository.save(user);
            
            PointHistory pointHistory = PointHistory.builder()
                    .user(user)
                    .points(points)
                    .operationType(OperationType.CHECKIN)
                    .description("Daily check-in points")
                    .build();
            pointHistoryRepository.save(pointHistory);
            
            LocalTime endOfDay = LocalTime.of(23, 59, 59);
            long secondsUntilEndOfDay = ChronoUnit.SECONDS.between(now, endOfDay);
            redisTemplate.opsForValue().set(checkinKey, true, secondsUntilEndOfDay, TimeUnit.SECONDS);
            
            // Invalidate checkin status cache for today
            String cacheKey = redisKeyFactory.generateCheckinStatusCacheKey(userId, today, today);
            redisTemplate.delete(cacheKey);
            
            response = CheckinResponse.builder()
                    .success(true)
                    .message("Check-in successful")
                    .pointsEarned(points)
                    .totalPoints(user.getLotusPoints())
                    .build();
            
            needRollback = false;
            
        } catch (Exception e) {
            // rollback redis manually
            if (needRollback) {
                try {
                    redisTemplate.opsForValue().set(monthlyKey, originalMonthlyCheckins);
                } catch (Exception ex) {
                    log.error("Failed to rollback Redis value: {}", ex.getMessage());
                }
            }
            
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new BadRequestException("Check-in operation was interrupted");
            } else if (e instanceof BadRequestException) {
                throw (BadRequestException) e;
            } else {
                throw new BadRequestException("Failed to check in: " + e.getMessage());
            }
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        
        return response;
    }
    
    @Override
    public CheckinStatusResponse getCheckinStatus(LocalDate startDate, LocalDate endDate) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        
        String cacheKey = redisKeyFactory.generateCheckinStatusCacheKey(userId, startDate, endDate);
        CheckinStatusResponse cachedStatus = (CheckinStatusResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStatus != null) {
            return cachedStatus;
        }
        
        List<Checkin> checkins = checkinRepository.findByUser_IdAndCheckinDateBetweenOrderByCheckinDateDesc(userId, startDate, endDate);
        
        Map<LocalDate, DailyStatus> statusMap = new HashMap<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            final LocalDate date = currentDate;
            Optional<Checkin> checkin = checkins.stream()
                    .filter(c -> c.getCheckinDate().equals(date))
                    .findFirst();
            
            if (checkin.isPresent()) {
                statusMap.put(date, DailyStatus.builder()
                        .date(date)
                        .checkedIn(true)
                        .pointsEarned(CheckinPoints.getPointsForDay(date.getDayOfWeek().getValue()))
                        .build());
            } else {
                statusMap.put(date, DailyStatus.builder()
                        .date(date)
                        .checkedIn(false)
                        .pointsEarned(0)
                        .build());
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        CheckinStatusResponse response = CheckinStatusResponse.builder()
                .statusMap(statusMap)
                .build();
        
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL_HOURS, TimeUnit.HOURS);
        
        return response;
    }
} 