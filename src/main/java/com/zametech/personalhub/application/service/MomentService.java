package com.zametech.personalhub.application.service;

import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Moment;
import com.zametech.personalhub.domain.repository.MomentRepository;
import com.zametech.personalhub.presentation.dto.request.CreateMomentRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateMomentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentRepository momentRepository;
    private final UserContextService userContextService;

    @Transactional
    public Moment createMoment(CreateMomentRequest request) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Moment moment = new Moment();
        moment.setContent(request.content());
        moment.setTags(request.tags());
        moment.setUserId(currentUserId);
        moment.setCreatedAt(LocalDateTime.now());
        moment.setUpdatedAt(LocalDateTime.now());

        log.info("Creating new moment for user: {}", currentUserId);
        return momentRepository.save(moment);
    }

    public Moment getMomentById(Long momentId) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Moment moment = momentRepository.findByIdAndUserId(momentId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Moment not found with id: " + momentId));
        
        log.info("Getting moment with id: {} for user: {}", momentId, currentUserId);
        return moment;
    }

    public Page<Moment> getMomentsByUser(Pageable pageable) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting moments for user: {} with pageable: {}", currentUserId, pageable);
        return momentRepository.findByUserId(currentUserId, pageable);
    }

    public Page<Moment> getMomentsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting moments for user: {} between {} and {}", currentUserId, startDate, endDate);
        return momentRepository.findByUserIdAndCreatedAtBetween(currentUserId, startDate, endDate, pageable);
    }

    public List<Moment> searchMoments(String query) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Searching moments for user: {} with query: {}", currentUserId, query);
        return momentRepository.searchByContent(currentUserId, query);
    }

    public List<Moment> getMomentsByTag(String tag) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Getting moments by tag: {} for user: {}", tag, currentUserId);
        return momentRepository.findByTag(currentUserId, tag);
    }

    public List<Moment> searchMomentsWithTag(String query, String tag) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        log.info("Searching moments for user: {} with query: {} and tag: {}", currentUserId, query, tag);
        return momentRepository.searchByContentAndTag(currentUserId, query, tag);
    }

    @Transactional
    public Moment updateMoment(Long momentId, UpdateMomentRequest request) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Moment moment = momentRepository.findByIdAndUserId(momentId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Moment not found with id: " + momentId));

        if (request.content() != null) {
            moment.setContent(request.content());
        }
        if (request.tags() != null) {
            moment.setTags(request.tags());
        }

        log.info("Updating moment with id: {} for user: {}", momentId, currentUserId);
        return momentRepository.save(moment);
    }

    @Transactional
    public void deleteMoment(Long momentId) {
        UUID currentUserId = userContextService.getCurrentUserId();
        
        Moment moment = momentRepository.findByIdAndUserId(momentId, currentUserId)
                .orElseThrow(() -> new TodoNotFoundException("Moment not found with id: " + momentId));

        log.info("Deleting moment with id: {} for user: {}", momentId, currentUserId);
        momentRepository.deleteById(momentId);
    }

    public List<String> getDefaultTags() {
        return Moment.DEFAULT_TAGS;
    }
}