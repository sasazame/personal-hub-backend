package com.zametech.todoapp.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfiguration {

    private String secret = "your-default-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm-security";
    private long expiration = 86400000;

    @Bean
    public JwtService jwtService() {
        return new JwtService(secret, Duration.ofMillis(expiration));
    }
}