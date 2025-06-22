package com.zametech.todoapp.application.service;

import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserContextService {

    private final UserRepository userRepository;

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("No authenticated user found");
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails)) {
            throw new RuntimeException("Invalid authentication principal");
        }

        UserDetails userDetails = (UserDetails) principal;
        String email = userDetails.getUsername();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
    
    // Temporary method for backward compatibility with Long-based repositories
    public Long getCurrentUserIdAsLong() {
        // This is a temporary workaround - in a real application, 
        // all repositories should be migrated to use UUID
        return getCurrentUser().getId().getMostSignificantBits();
    }

    public boolean isCurrentUser(UUID userId) {
        return getCurrentUserId().equals(userId);
    }
}