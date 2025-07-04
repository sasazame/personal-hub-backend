package com.zametech.personalhub.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("docker")
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        logger.info("Original DATABASE_URL: {}", databaseUrl);
        
        HikariConfig config = new HikariConfig();
        
        if (databaseUrl != null && !databaseUrl.isEmpty()) {
            // Convert render.com PostgreSQL URL format to JDBC format
            if (databaseUrl.startsWith("postgresql://")) {
                // Format: postgresql://user:password@host:port/database
                String jdbcUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://");
                
                // Extract username and password if present in URL
                if (jdbcUrl.contains("@")) {
                    String[] parts = jdbcUrl.split("@");
                    String userInfo = parts[0].substring("jdbc:postgresql://".length());
                    String hostInfo = parts[1];
                    
                    if (userInfo.contains(":")) {
                        String[] credentials = userInfo.split(":");
                        config.setUsername(credentials[0]);
                        config.setPassword(credentials[1]);
                        logger.info("Extracted username: {}", credentials[0]);
                    }
                    
                    config.setJdbcUrl("jdbc:postgresql://" + hostInfo);
                    logger.info("Converted JDBC URL: jdbc:postgresql://{}", hostInfo);
                } else {
                    config.setJdbcUrl(jdbcUrl);
                    logger.info("Setting JDBC URL: {}", jdbcUrl);
                }
            } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
                // Already in correct format
                config.setJdbcUrl(databaseUrl);
                logger.info("URL already in JDBC format: {}", databaseUrl);
            }
            
            // Override with explicit username/password if provided
            String username = System.getenv("DATABASE_USERNAME");
            String password = System.getenv("DATABASE_PASSWORD");
            
            if (username != null && !username.isEmpty()) {
                config.setUsername(username);
                logger.info("Override username from DATABASE_USERNAME");
            }
            if (password != null && !password.isEmpty()) {
                config.setPassword(password);
                logger.info("Override password from DATABASE_PASSWORD");
            }
        }
        
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        logger.info("Final JDBC URL: {}", config.getJdbcUrl());
        logger.info("Final username: {}", config.getUsername());
        
        return new HikariDataSource(config);
    }
}