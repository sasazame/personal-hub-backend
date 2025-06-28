package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.infrastructure.persistence.entity.AuthorizationCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface JpaAuthorizationCodeRepository extends JpaRepository<AuthorizationCodeEntity, String> {
    Optional<AuthorizationCodeEntity> findByCode(String code);
    
    @Modifying
    @Query("DELETE FROM AuthorizationCodeEntity a WHERE a.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);
    
    void deleteByCode(String code);
}