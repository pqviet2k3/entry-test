package com.wiinvent.entrytest.repository;

import com.wiinvent.entrytest.model.PointHistory;
import com.wiinvent.entrytest.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
    
    Page<PointHistory> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
} 