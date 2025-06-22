package com.zametech.todoapp.infrastructure.persistence.jpa;

import com.zametech.todoapp.domain.model.AuthorizationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface JpaAuthorizationCodeRepository extends JpaRepository<AuthorizationCode, String> {
    Optional<AuthorizationCode> findByCode(String code);
    
    @Modifying
    @Query("DELETE FROM AuthorizationCode a WHERE a.expiresAt < :now")
    void deleteExpiredCodes(LocalDateTime now);
    
    void deleteByCode(String code);
}