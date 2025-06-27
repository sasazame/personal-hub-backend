package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.domain.model.GoalStatus;
import com.zametech.todoapp.domain.model.GoalType;
import com.zametech.todoapp.infrastructure.persistence.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaGoalRepository extends JpaRepository<GoalEntity, Long> {
    List<GoalEntity> findByUserId(UUID userId);
    List<GoalEntity> findByUserIdAndStatus(UUID userId, GoalStatus status);
    List<GoalEntity> findByUserIdAndGoalType(UUID userId, GoalType goalType);
    
    @Query("SELECT g FROM GoalEntity g WHERE g.userId = :userId AND g.status = 'ACTIVE' " +
           "AND g.startDate <= :endDate AND g.endDate >= :startDate")
    List<GoalEntity> findActiveGoalsByUserIdAndDateRange(@Param("userId") UUID userId, 
                                                         @Param("startDate") LocalDate startDate, 
                                                         @Param("endDate") LocalDate endDate);
    
    boolean existsByIdAndUserId(Long id, UUID userId);
}