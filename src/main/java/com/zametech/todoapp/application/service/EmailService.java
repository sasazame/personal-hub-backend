package com.zametech.todoapp.application.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetToken);
}