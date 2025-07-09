package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.MomentNotFoundException;
import com.zametech.personalhub.domain.model.Moment;
import com.zametech.personalhub.domain.repository.MomentRepository;
import com.zametech.personalhub.presentation.dto.request.CreateMomentRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateMomentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MomentServiceTest {

    @Mock
    private MomentRepository momentRepository;

    @Mock
    private UserContextService userContextService;

    @InjectMocks
    private MomentService momentService;

    private UUID userId;
    private Moment moment;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        
        moment = new Moment();
        moment.setId(1L);
        moment.setContent("Test moment content");
        moment.setTags("Ideas,test");
        moment.setUserId(userId);
        moment.setCreatedAt(LocalDateTime.now());
        moment.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createMoment_withValidRequest_shouldCreateAndReturnMoment() {
        // Given
        CreateMomentRequest request = new CreateMomentRequest(
            "Just had a great idea about the project architecture",
            Arrays.asList("Ideas", "work")
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
            Moment savedMoment = invocation.getArgument(0);
            savedMoment.setId(2L);
            return savedMoment;
        });

        // When
        Moment result = momentService.createMoment(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("Just had a great idea about the project architecture");
        assertThat(result.getTags()).isEqualTo("Ideas,work");
        assertThat(result.getUserId()).isEqualTo(userId);
        verify(momentRepository).save(any(Moment.class));
    }

    @Test
    void createMoment_withEmptyTags_shouldCreateMomentWithEmptyTags() {
        // Given
        CreateMomentRequest request = new CreateMomentRequest(
            "Simple moment without tags",
            Arrays.asList()
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.save(any(Moment.class))).thenAnswer(invocation -> {
            Moment savedMoment = invocation.getArgument(0);
            savedMoment.setId(3L);
            return savedMoment;
        });

        // When
        Moment result = momentService.createMoment(request);

        // Then
        assertThat(result.getTags()).isEqualTo("");
    }

    @Test
    void getMomentById_withValidId_shouldReturnMoment() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(moment));

        // When
        Moment result = momentService.getMomentById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getContent()).isEqualTo("Test moment content");
    }

    @Test
    void getMomentById_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> momentService.getMomentById(999L))
            .isInstanceOf(MomentNotFoundException.class)
            .hasMessageContaining("Moment not found with id: 999");
    }

    @Test
    void getMomentById_fromDifferentUser_shouldThrowNotFoundException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(momentRepository.findByIdAndUserId(1L, otherUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> momentService.getMomentById(1L))
            .isInstanceOf(MomentNotFoundException.class)
            .hasMessageContaining("Moment not found with id: 1");
    }

    @Test
    void getMomentsByUser_shouldReturnPagedMoments() {
        // Given
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Moment> momentList = Arrays.asList(moment);
        Page<Moment> momentPage = new PageImpl<>(momentList, pageRequest, 1);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByUserId(userId, pageRequest)).thenReturn(momentPage);

        // When
        Page<Moment> result = momentService.getMomentsByUser(pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).isEqualTo("Test moment content");
    }

    @Test
    void getMomentsByDateRange_shouldReturnPagedMoments() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        PageRequest pageRequest = PageRequest.of(0, 10);
        List<Moment> momentList = Arrays.asList(moment);
        Page<Moment> momentPage = new PageImpl<>(momentList, pageRequest, 1);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageRequest))
            .thenReturn(momentPage);

        // When
        Page<Moment> result = momentService.getMomentsByDateRange(startDate, endDate, pageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(momentRepository).findByUserIdAndCreatedAtBetween(userId, startDate, endDate, pageRequest);
    }

    @Test
    void searchMoments_withQuery_shouldReturnMatchingMoments() {
        // Given
        String query = "test";
        Moment moment2 = new Moment();
        moment2.setId(2L);
        moment2.setContent("Another test moment");
        moment2.setUserId(userId);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.searchByContent(userId, query))
            .thenReturn(Arrays.asList(moment, moment2));

        // When
        List<Moment> result = momentService.searchMoments(query);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Moment::getContent)
            .containsExactly("Test moment content", "Another test moment");
    }

    @Test
    void searchMoments_withNoMatches_shouldReturnEmptyList() {
        // Given
        String query = "nonexistent";
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.searchByContent(userId, query))
            .thenReturn(Arrays.asList());

        // When
        List<Moment> result = momentService.searchMoments(query);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getMomentsByTag_shouldReturnMomentsWithTag() {
        // Given
        String tag = "Ideas";
        Moment moment2 = new Moment();
        moment2.setId(2L);
        moment2.setContent("Another idea");
        moment2.setTags("Ideas,Discoveries");
        moment2.setUserId(userId);
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByTag(userId, tag))
            .thenReturn(Arrays.asList(moment, moment2));

        // When
        List<Moment> result = momentService.getMomentsByTag(tag);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).hasTag("Ideas")).isTrue();
        assertThat(result.get(1).hasTag("Ideas")).isTrue();
    }

    @Test
    void searchMomentsWithTag_shouldReturnMatchingMomentsWithTag() {
        // Given
        String query = "test";
        String tag = "Ideas";
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.searchByContentAndTag(userId, query, tag))
            .thenReturn(Arrays.asList(moment));

        // When
        List<Moment> result = momentService.searchMomentsWithTag(query, tag);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).contains("Test");
        assertThat(result.get(0).hasTag("Ideas")).isTrue();
    }

    @Test
    void updateMoment_withFullUpdate_shouldUpdateAllFields() {
        // Given
        UpdateMomentRequest request = new UpdateMomentRequest(
            "Updated moment content",
            Arrays.asList("Emotions", "Log")
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(moment));
        when(momentRepository.save(any(Moment.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Moment result = momentService.updateMoment(1L, request);

        // Then
        assertThat(result.getContent()).isEqualTo("Updated moment content");
        assertThat(result.getTags()).isEqualTo("Emotions,Log");
        verify(momentRepository).save(moment);
    }

    @Test
    void updateMoment_withPartialUpdate_shouldUpdateOnlyProvidedFields() {
        // Given
        UpdateMomentRequest request = new UpdateMomentRequest(
            "Only content updated",
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(moment));
        when(momentRepository.save(any(Moment.class))).thenAnswer(i -> i.getArguments()[0]);

        // When
        Moment result = momentService.updateMoment(1L, request);

        // Then
        assertThat(result.getContent()).isEqualTo("Only content updated");
        assertThat(result.getTags()).isEqualTo("Ideas,test"); // Unchanged
        verify(momentRepository).save(moment);
    }

    @Test
    void updateMoment_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        UpdateMomentRequest request = new UpdateMomentRequest(
            "Updated content",
            null
        );
        
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> momentService.updateMoment(999L, request))
            .isInstanceOf(MomentNotFoundException.class)
            .hasMessageContaining("Moment not found with id: 999");
    }

    @Test
    void deleteMoment_withValidId_shouldDeleteMoment() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(moment));

        // When
        momentService.deleteMoment(1L);

        // Then
        verify(momentRepository).deleteById(1L);
    }

    @Test
    void deleteMoment_withNonExistentId_shouldThrowNotFoundException() {
        // Given
        when(userContextService.getCurrentUserId()).thenReturn(userId);
        when(momentRepository.findByIdAndUserId(999L, userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> momentService.deleteMoment(999L))
            .isInstanceOf(MomentNotFoundException.class)
            .hasMessageContaining("Moment not found with id: 999");
    }

    @Test
    void deleteMoment_fromDifferentUser_shouldThrowNotFoundException() {
        // Given
        UUID otherUserId = UUID.randomUUID();
        when(userContextService.getCurrentUserId()).thenReturn(otherUserId);
        when(momentRepository.findByIdAndUserId(1L, otherUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> momentService.deleteMoment(1L))
            .isInstanceOf(MomentNotFoundException.class)
            .hasMessageContaining("Moment not found with id: 1");
    }

    @Test
    void getDefaultTags_shouldReturnExpectedDefaultTags() {
        // When
        List<String> defaultTags = momentService.getDefaultTags();

        // Then
        assertThat(defaultTags).containsExactly("Ideas", "Discoveries", "Emotions", "Log", "Other");
    }
}