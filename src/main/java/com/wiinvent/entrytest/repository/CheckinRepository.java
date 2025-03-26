package com.wiinvent.entrytest.repository;

import com.wiinvent.entrytest.model.Checkin;
import com.wiinvent.entrytest.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckinRepository extends JpaRepository<Checkin, Long> {
    
    Optional<Checkin> findByUserAndCheckinDate(User user, LocalDate date);
    
    boolean existsByUserAndCheckinDate(User user, LocalDate date);
    
    List<Checkin> findByUserOrderByCheckinDateDesc(User user);
    
    @Query("SELECT c.checkinDate FROM Checkin c WHERE c.user.id = :userId AND c.checkinDate BETWEEN :startDate AND :endDate")
    List<LocalDate> findCheckinDatesByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    
    long countByUserAndCheckinDateBetween(User user, LocalDate startDate, LocalDate endDate);
    
    List<Checkin> findByUserAndCheckinDateBetweenOrderByCheckinDateDesc(User user, LocalDate startDate, LocalDate endDate);

    boolean existsByUser_IdAndCheckinDate(Long userId, LocalDate checkinDate);
    long countByUser_IdAndCheckinDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Checkin> findByUser_IdAndCheckinDateBetweenOrderByCheckinDateDesc(Long userId, LocalDate startDate, LocalDate endDate);
} 