package com.zametech.todoapp.domain.repository;

import com.zametech.todoapp.domain.model.AuthorizationCode;
import java.util.Optional;

public interface AuthorizationCodeRepository {
    Optional<AuthorizationCode> findByCode(String code);
    AuthorizationCode save(AuthorizationCode authorizationCode);
    void deleteByCode(String code);
    void deleteExpiredCodes();
}