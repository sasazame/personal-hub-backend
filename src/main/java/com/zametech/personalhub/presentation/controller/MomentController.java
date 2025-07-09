package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.MomentService;
import com.zametech.personalhub.domain.model.Moment;
import com.zametech.personalhub.presentation.dto.request.CreateMomentRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateMomentRequest;
import com.zametech.personalhub.presentation.dto.response.MomentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    @PostMapping
    public ResponseEntity<MomentResponse> createMoment(@Valid @RequestBody CreateMomentRequest request) {
        log.info("Creating moment");
        Moment moment = momentService.createMoment(request);
        MomentResponse response = mapToMomentResponse(moment);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MomentResponse> getMoment(@PathVariable Long id) {
        log.info("Getting moment with id: {}", id);
        Moment moment = momentService.getMomentById(id);
        MomentResponse response = mapToMomentResponse(moment);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<MomentResponse>> getMoments(Pageable pageable) {
        log.info("Getting moments with pageable: {}", pageable);
        Page<Moment> moments = momentService.getMomentsByUser(pageable);
        Page<MomentResponse> response = moments.map(this::mapToMomentResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/range")
    public ResponseEntity<Page<MomentResponse>> getMomentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        log.info("Getting moments between {} and {}", startDate, endDate);
        Page<Moment> moments = momentService.getMomentsByDateRange(startDate, endDate, pageable);
        Page<MomentResponse> response = moments.map(this::mapToMomentResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MomentResponse>> searchMoments(
            @RequestParam String query,
            @RequestParam(required = false) String tag) {
        log.info("Searching moments with query: {} and tag: {}", query, tag);
        
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<Moment> moments;
        if (tag != null && !tag.isEmpty()) {
            moments = momentService.searchMomentsWithTag(query, tag);
        } else {
            moments = momentService.searchMoments(query);
        }
        
        List<MomentResponse> response = moments.stream()
                .map(this::mapToMomentResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tag/{tag}")
    public ResponseEntity<List<MomentResponse>> getMomentsByTag(@PathVariable String tag) {
        log.info("Getting moments by tag: {}", tag);
        List<Moment> moments = momentService.getMomentsByTag(tag);
        List<MomentResponse> response = moments.stream()
                .map(this::mapToMomentResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tags/default")
    public ResponseEntity<List<String>> getDefaultTags() {
        log.info("Getting default tags");
        List<String> tags = momentService.getDefaultTags();
        return ResponseEntity.ok(tags);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MomentResponse> updateMoment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMomentRequest request) {
        log.info("Updating moment with id: {}", id);
        Moment moment = momentService.updateMoment(id, request);
        MomentResponse response = mapToMomentResponse(moment);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMoment(@PathVariable Long id) {
        log.info("Deleting moment with id: {}", id);
        momentService.deleteMoment(id);
        return ResponseEntity.noContent().build();
    }

    private MomentResponse mapToMomentResponse(Moment moment) {
        return new MomentResponse(
                moment.getId(),
                moment.getContent(),
                moment.getTagList(),
                moment.getCreatedAt(),
                moment.getUpdatedAt()
        );
    }
}