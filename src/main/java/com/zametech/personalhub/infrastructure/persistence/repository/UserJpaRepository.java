package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {
    
    Optional<UserEntity> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<UserEntity> findByUsername(String username);
    
    boolean existsByUsername(String username);
}