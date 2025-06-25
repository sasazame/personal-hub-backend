package com.zametech.todoapp.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import com.zametech.todoapp.TestcontainersConfiguration;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = {
    "app.security.jwt.secret-key=test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm",
    "app.security.jwt.expiration=3600000",
    "app.security.jwt.key-id=test-key"
})
class JwtConfigurationTest {

    @Autowired
    private JwtConfiguration jwtConfiguration;

    @Test
    void shouldLoadJwtConfigurationFromProperties() {
        assertNotNull(jwtConfiguration);
        assertEquals("test-secret-key-that-is-at-least-256-bits-long-for-HS256-algorithm", 
                     jwtConfiguration.getSecretKey());
        assertEquals(3600000, jwtConfiguration.getExpiration());
        assertEquals("test-key", jwtConfiguration.getKeyId());
    }

    @Test
    void shouldHaveValidConfiguration() {
        // JwtService is now @Service component, not created by configuration bean
        assertNotNull(jwtConfiguration.getSecretKey());
        assertTrue(jwtConfiguration.getExpiration() > 0);
        assertNotNull(jwtConfiguration.getKeyId());
    }
}