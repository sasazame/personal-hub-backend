package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.User;

import java.util.Optional;

public interface UserRepository {
    
    Optional<User> findByEmail(String email);
    
    User save(User user);
    
    boolean existsByEmail(String email);
    
    Optional<User> findById(Long id);
    
    boolean existsByUsername(String username);
    
    void deleteById(Long id);
    
    Optional<User> findByUsername(String username);
}