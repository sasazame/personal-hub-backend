package com.zametech.todoapp.infrastructure.persistence.repository;

import com.zametech.todoapp.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    
    Optional<UserEntity> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    Optional<UserEntity> findByUsername(String username);
    
    boolean existsByUsername(String username);
}