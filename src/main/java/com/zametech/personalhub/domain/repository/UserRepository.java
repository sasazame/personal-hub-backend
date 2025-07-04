package com.zametech.personalhub.domain.repository;

import com.zametech.personalhub.domain.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    
    Optional<User> findByEmail(String email);
    
    User save(User user);
    
    boolean existsByEmail(String email);
    
    Optional<User> findById(UUID id);
    
    boolean existsByUsername(String username);
    
    void deleteById(UUID id);
    
    Optional<User> findByUsername(String username);
}