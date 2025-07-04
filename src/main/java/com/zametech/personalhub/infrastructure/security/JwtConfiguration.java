package com.zametech.personalhub.infrastructure.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtConfiguration {

    private String secretKey = "your-default-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm-security";
    private long expiration = 900000;
    private String keyId = "default-key";

    // JwtService is now defined as @Service component with @RequiredArgsConstructor
    // No need for @Bean definition here
}