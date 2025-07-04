package com.zametech.personalhub.application.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
}