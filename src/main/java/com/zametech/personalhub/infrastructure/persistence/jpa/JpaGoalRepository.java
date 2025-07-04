package com.zametech.personalhub.infrastructure.persistence.jpa;

import com.zametech.personalhub.domain.model.GoalType;
import com.zametech.personalhub.infrastructure.persistence.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaGoalRepository extends JpaRepository<GoalEntity, Long> {
    List<GoalEntity> findByUserId(UUID userId);
    List<GoalEntity> findByUserIdAndIsActive(UUID userId, Boolean isActive);
    List<GoalEntity> findByUserIdAndGoalType(UUID userId, GoalType goalType);
    List<GoalEntity> findByUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(UUID userId, LocalDate startDate, LocalDate endDate);
    boolean existsByIdAndUserId(Long id, UUID userId);
}