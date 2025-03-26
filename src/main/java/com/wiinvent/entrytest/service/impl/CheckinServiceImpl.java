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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
    @Transactional
    public CheckinResponse checkin() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getId();
        
        //Distributed lock
        String lockKey = "checkin:lock:" + userId + ":" + today;
        RLock lock = redissonClient.getLock(lockKey);
        
        CheckinResponse response;
        try {
            if (!lock.tryLock(5, TimeUnit.SECONDS)) {
                throw new BadRequestException("Another check-in operation is in progress");
            }
            
            if (checkinRepository.existsByUser_IdAndCheckinDate(userId, today)) {
                throw new BadRequestException("You have already checked in today");
            }
            
            if (!isTimeInAllowedRanges(now)) {
                throw new BadRequestException("Check-in is only allowed during morning (9:00-11:00) or evening (19:00-21:00) hours");
            }
            
            LocalDate firstDayOfMonth = today.withDayOfMonth(1);
            LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            long monthlyCheckins = checkinRepository.countByUser_IdAndCheckinDateBetween(userId, firstDayOfMonth, lastDayOfMonth);
            
            if (monthlyCheckins >= maxCheckinsPerMonth) {
                throw new BadRequestException("You have reached the maximum number of check-ins for this month");
            }
            
            Checkin checkin = Checkin.builder()
                    .user(userRepository.findById(userId)
                            .orElseThrow(() -> new BadRequestException("User not found")))
                    .checkinDate(today)
                    .checkinTime(now)
                    .build();
            
            checkinRepository.save(checkin);
            
            int points = CheckinPoints.getPointsForDay(today.getDayOfWeek().getValue());
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
            
            response = CheckinResponse.builder()
                    .success(true)
                    .message("Check-in successful")
                    .pointsEarned(points)
                    .totalPoints(user.getLotusPoints())
                    .build();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Check-in operation was interrupted");
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
        
        return CheckinStatusResponse.builder()
                .statusMap(statusMap)
                .build();
    }
} 