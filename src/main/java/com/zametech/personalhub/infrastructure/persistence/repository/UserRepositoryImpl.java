package com.zametech.personalhub.infrastructure.persistence.repository;

import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.repository.UserRepository;
import com.zametech.personalhub.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Optional<User> findByEmail(String email) {
        return userJpaRepository.findByEmail(email).map(this::toModel);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity savedEntity = userJpaRepository.save(entity);
        return toModel(savedEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userJpaRepository.findById(id).map(this::toModel);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userJpaRepository.existsByUsername(username);
    }

    @Override
    public void deleteById(UUID id) {
        userJpaRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userJpaRepository.findByUsername(username).map(this::toModel);
    }

    private User toModel(UserEntity entity) {
        User user = new User();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setPassword(entity.getPassword());
        user.setUsername(entity.getUsername());
        user.setEnabled(entity.isEnabled());
        user.setEmailVerified(entity.getEmailVerified());
        user.setProfilePictureUrl(entity.getProfilePictureUrl());
        user.setGivenName(entity.getGivenName());
        user.setFamilyName(entity.getFamilyName());
        user.setLocale(entity.getLocale());
        user.setWeekStartDay(entity.getWeekStartDay());
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());
        return user;
    }

    private UserEntity toEntity(User user) {
        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setPassword(user.getPassword());
        entity.setUsername(user.getUsername());
        entity.setEnabled(user.isEnabled());
        entity.setEmailVerified(user.getEmailVerified());
        entity.setProfilePictureUrl(user.getProfilePictureUrl());
        entity.setGivenName(user.getGivenName());
        entity.setFamilyName(user.getFamilyName());
        entity.setLocale(user.getLocale());
        entity.setWeekStartDay(user.getWeekStartDay());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());
        return entity;
    }
}