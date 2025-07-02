package com.zametech.todoapp.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StateParameterServiceTest {

    @InjectMocks
    private StateParameterService stateParameterService;

    private StateParameterService spyService;

    @BeforeEach
    void setUp() {
        spyService = spy(stateParameterService);
    }

    @Test
    void generateState_CreatesBase64EncodedState() {
        // When
        String state = stateParameterService.generateState();

        // Then
        assertThat(state).isNotNull();
        assertThat(state).isNotEmpty();
        
        // Verify it's valid base64
        byte[] decoded = Base64.getUrlDecoder().decode(state);
        assertThat(decoded).hasSize(32); // STATE_LENGTH = 32
    }

    @Test
    void generateState_ProducesUniqueStates() {
        // When
        String state1 = stateParameterService.generateState();
        String state2 = stateParameterService.generateState();

        // Then
        assertThat(state1).isNotEqualTo(state2);
    }

    @Test
    void storeState_CreatesStateData() {
        // Given
        String state = "test-state";
        String provider = "google";
        String ipAddress = "192.168.1.1";

        // When
        StateParameterService.StateData stateData = stateParameterService.storeState(state, provider, ipAddress);

        // Then
        assertThat(stateData).isNotNull();
        assertThat(stateData.state()).isEqualTo(state);
        assertThat(stateData.provider()).isEqualTo(provider);
        assertThat(stateData.ipAddress()).isEqualTo(ipAddress);
        assertThat(stateData.timestamp()).isGreaterThan(0);
    }

    @Test
    void validateState_WithValidState_ReturnsTrue() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        long currentTime = System.currentTimeMillis();
        
        StateParameterService.StateData stateData = new StateParameterService.StateData(
            state, provider, ipAddress, currentTime
        );
        
        doReturn(stateData).when(spyService).getStateData(state);

        // When
        boolean result = spyService.validateState(state, provider, ipAddress);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void validateState_WithNullStateData_ReturnsFalse() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        
        doReturn(null).when(spyService).getStateData(state);

        // When
        boolean result = spyService.validateState(state, provider, ipAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateState_WithExpiredState_ReturnsFalse() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        long expiredTime = System.currentTimeMillis() - 700000; // 11+ minutes ago
        
        StateParameterService.StateData stateData = new StateParameterService.StateData(
            state, provider, ipAddress, expiredTime
        );
        
        doReturn(stateData).when(spyService).getStateData(state);

        // When
        boolean result = spyService.validateState(state, provider, ipAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateState_WithWrongProvider_ReturnsFalse() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        long currentTime = System.currentTimeMillis();
        
        StateParameterService.StateData stateData = new StateParameterService.StateData(
            state, "google", ipAddress, currentTime // Different provider
        );
        
        doReturn(stateData).when(spyService).getStateData(state);

        // When
        boolean result = spyService.validateState(state, provider, ipAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void validateState_WithDifferentIpAddress_ReturnsTrue() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        String differentIp = "192.168.1.2";
        long currentTime = System.currentTimeMillis();
        
        StateParameterService.StateData stateData = new StateParameterService.StateData(
            state, provider, differentIp, currentTime
        );
        
        doReturn(stateData).when(spyService).getStateData(state);

        // When
        boolean result = spyService.validateState(state, provider, ipAddress);

        // Then
        // Should still return true as IP mismatch only logs warning
        assertThat(result).isTrue();
    }

    @Test
    void validateState_WithException_ReturnsFalse() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        
        doThrow(new RuntimeException("Cache error")).when(spyService).getStateData(state);

        // When
        boolean result = spyService.validateState(state, provider, ipAddress);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void generateAndStoreState_GeneratesAndStoresState() {
        // Given
        String provider = "google";
        String ipAddress = "192.168.1.1";

        // When
        String state = stateParameterService.generateAndStoreState(provider, ipAddress);

        // Then
        assertThat(state).isNotNull();
        assertThat(state).isNotEmpty();
        
        // Verify it's valid base64
        byte[] decoded = Base64.getUrlDecoder().decode(state);
        assertThat(decoded).hasSize(32);
    }

    @Test
    void getStateData_ReturnsCachedData() {
        // Given
        String state = "test-state";
        
        // When
        StateParameterService.StateData result = stateParameterService.getStateData(state);

        // Then
        // This method relies on Spring Cache, so it returns null in unit test
        assertThat(result).isNull();
    }

    @Test
    void stateData_RecordProperties() {
        // Given
        String state = "test-state";
        String provider = "github";
        String ipAddress = "192.168.1.1";
        long timestamp = System.currentTimeMillis();

        // When
        StateParameterService.StateData stateData = new StateParameterService.StateData(
            state, provider, ipAddress, timestamp
        );

        // Then
        assertThat(stateData.state()).isEqualTo(state);
        assertThat(stateData.provider()).isEqualTo(provider);
        assertThat(stateData.ipAddress()).isEqualTo(ipAddress);
        assertThat(stateData.timestamp()).isEqualTo(timestamp);
    }
}