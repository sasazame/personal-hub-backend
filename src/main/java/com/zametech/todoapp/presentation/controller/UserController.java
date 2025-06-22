package com.zametech.todoapp.presentation.controller;

import com.zametech.todoapp.application.service.UserService;
import com.zametech.todoapp.domain.model.User;
import com.zametech.todoapp.presentation.dto.request.ChangePasswordRequest;
import com.zametech.todoapp.presentation.dto.request.UpdateUserRequest;
import com.zametech.todoapp.presentation.dto.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserProfile(@PathVariable UUID id) {
        log.info("Getting user profile for id: {}", id);
        User user = userService.getUserById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        UserResponse response = mapToUserResponse(user);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user profile for id: {}", id);
        User updatedUser = userService.updateUserProfile(id, request);
        UserResponse response = mapToUserResponse(updatedUser);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable UUID id,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Changing password for user id: {}", id);
        userService.changePassword(id, request);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        log.info("Deleting user account for id: {}", id);
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
    
    private UserResponse mapToUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}