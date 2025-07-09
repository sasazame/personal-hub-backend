package com.zametech.personalhub.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.service.MomentService;
import com.zametech.personalhub.common.exception.TodoNotFoundException;
import com.zametech.personalhub.domain.model.Moment;
import com.zametech.personalhub.presentation.dto.request.CreateMomentRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateMomentRequest;
import com.zametech.personalhub.presentation.dto.response.MomentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = MomentController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class MomentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MomentService momentService;

    private Moment sampleMoment;
    private CreateMomentRequest createMomentRequest;
    private UpdateMomentRequest updateMomentRequest;

    @BeforeEach
    void setUp() {
        sampleMoment = new Moment();
        sampleMoment.setId(1L);
        sampleMoment.setContent("Just had a great idea about the project");
        sampleMoment.setTags(Arrays.asList("Ideas", "work"));
        sampleMoment.setUserId(UUID.randomUUID());
        sampleMoment.setCreatedAt(LocalDateTime.now());
        sampleMoment.setUpdatedAt(LocalDateTime.now());

        createMomentRequest = new CreateMomentRequest(
            "New moment content",
            Arrays.asList("Ideas", "test")
        );

        updateMomentRequest = new UpdateMomentRequest(
            "Updated moment content",
            Arrays.asList("Emotions", "Log")
        );
    }

    @Test
    @WithMockUser
    void createMoment_withValidRequest_shouldReturnCreatedMoment() throws Exception {
        when(momentService.createMoment(any(CreateMomentRequest.class))).thenReturn(sampleMoment);

        mockMvc.perform(post("/api/v1/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createMomentRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.content").value("Just had a great idea about the project"))
            .andExpect(jsonPath("$.tags", hasSize(2)))
            .andExpect(jsonPath("$.tags[0]").value("Ideas"))
            .andExpect(jsonPath("$.tags[1]").value("work"));

        verify(momentService).createMoment(any(CreateMomentRequest.class));
    }

    @Test
    @WithMockUser
    void createMoment_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        CreateMomentRequest invalidRequest = new CreateMomentRequest(
            "", // Empty content
            null
        );

        mockMvc.perform(post("/api/v1/moments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        verify(momentService, never()).createMoment(any());
    }

    @Test
    @WithMockUser
    void getMoment_withValidId_shouldReturnMoment() throws Exception {
        when(momentService.getMomentById(1L)).thenReturn(sampleMoment);

        mockMvc.perform(get("/api/v1/moments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.content").value("Just had a great idea about the project"));

        verify(momentService).getMomentById(1L);
    }

    @Test
    @WithMockUser
    void getMoment_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(momentService.getMomentById(999L)).thenThrow(new TodoNotFoundException("Moment not found with id: 999"));

        mockMvc.perform(get("/api/v1/moments/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Moment not found with id: 999"));

        verify(momentService).getMomentById(999L);
    }

    @Test
    @WithMockUser
    void getMoment_withAccessDenied_shouldReturnForbidden() throws Exception {
        when(momentService.getMomentById(1L)).thenThrow(new AccessDeniedException("Access denied"));

        mockMvc.perform(get("/api/v1/moments/1"))
            .andExpect(status().isForbidden());

        verify(momentService).getMomentById(1L);
    }

    @Test
    @WithMockUser
    void getMoments_withPagination_shouldReturnPagedMoments() throws Exception {
        List<Moment> momentList = Arrays.asList(sampleMoment);
        Page<Moment> momentPage = new PageImpl<>(momentList, PageRequest.of(0, 10), 1);
        
        when(momentService.getMomentsByUser(any(Pageable.class))).thenReturn(momentPage);

        mockMvc.perform(get("/api/v1/moments")
                .param("page", "0")
                .param("size", "10")
                .param("sort", "createdAt,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(1))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0));

        verify(momentService).getMomentsByUser(any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getMoments_withEmptyResult_shouldReturnEmptyPage() throws Exception {
        Page<Moment> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        
        when(momentService.getMomentsByUser(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/v1/moments"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)))
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(momentService).getMomentsByUser(any(Pageable.class));
    }

    @Test
    @WithMockUser
    void getMomentsByDateRange_shouldReturnPagedMoments() throws Exception {
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        List<Moment> momentList = Arrays.asList(sampleMoment);
        Page<Moment> momentPage = new PageImpl<>(momentList, PageRequest.of(0, 10), 1);
        
        when(momentService.getMomentsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class)))
            .thenReturn(momentPage);

        mockMvc.perform(get("/api/v1/moments/range")
                .param("startDate", startDate.format(DateTimeFormatter.ISO_DATE_TIME))
                .param("endDate", endDate.format(DateTimeFormatter.ISO_DATE_TIME))
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(1));

        verify(momentService).getMomentsByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), any(Pageable.class));
    }

    @Test
    @WithMockUser
    void searchMoments_withQuery_shouldReturnMatchingMoments() throws Exception {
        List<Moment> searchResults = Arrays.asList(sampleMoment);
        when(momentService.searchMoments("test")).thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/moments/search")
                .param("query", "test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].content").value("Just had a great idea about the project"));

        verify(momentService).searchMoments("test");
    }

    @Test
    @WithMockUser
    void searchMoments_withQueryAndTag_shouldReturnMatchingMoments() throws Exception {
        List<Moment> searchResults = Arrays.asList(sampleMoment);
        when(momentService.searchMomentsWithTag("test", "Ideas")).thenReturn(searchResults);

        mockMvc.perform(get("/api/v1/moments/search")
                .param("query", "test")
                .param("tag", "Ideas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].content").value("Just had a great idea about the project"));

        verify(momentService).searchMomentsWithTag("test", "Ideas");
    }

    @Test
    @WithMockUser
    void searchMoments_withEmptyQuery_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/moments/search")
                .param("query", ""))
            .andExpect(status().isBadRequest());

        verify(momentService, never()).searchMoments(anyString());
    }

    @Test
    @WithMockUser
    void getMomentsByTag_shouldReturnMomentsWithTag() throws Exception {
        List<Moment> moments = Arrays.asList(sampleMoment);
        when(momentService.getMomentsByTag("Ideas")).thenReturn(moments);

        mockMvc.perform(get("/api/v1/moments/tag/Ideas"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].tags[0]").value("Ideas"));

        verify(momentService).getMomentsByTag("Ideas");
    }

    @Test
    @WithMockUser
    void getDefaultTags_shouldReturnDefaultTags() throws Exception {
        List<String> defaultTags = Arrays.asList("Ideas", "Discoveries", "Emotions", "Log", "Other");
        when(momentService.getDefaultTags()).thenReturn(defaultTags);

        mockMvc.perform(get("/api/v1/moments/tags/default"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(5)))
            .andExpect(jsonPath("$[0]").value("Ideas"))
            .andExpect(jsonPath("$[1]").value("Discoveries"))
            .andExpect(jsonPath("$[2]").value("Emotions"))
            .andExpect(jsonPath("$[3]").value("Log"))
            .andExpect(jsonPath("$[4]").value("Other"));

        verify(momentService).getDefaultTags();
    }

    @Test
    @WithMockUser
    void updateMoment_withValidRequest_shouldReturnUpdatedMoment() throws Exception {
        Moment updatedMoment = new Moment();
        updatedMoment.setId(1L);
        updatedMoment.setContent("Updated moment content");
        updatedMoment.setTags("Emotions,Log");
        updatedMoment.setUserId(sampleMoment.getUserId());
        updatedMoment.setCreatedAt(sampleMoment.getCreatedAt());
        updatedMoment.setUpdatedAt(LocalDateTime.now());

        when(momentService.updateMoment(eq(1L), any(UpdateMomentRequest.class))).thenReturn(updatedMoment);

        mockMvc.perform(put("/api/v1/moments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateMomentRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.content").value("Updated moment content"))
            .andExpect(jsonPath("$.tags[0]").value("Emotions"))
            .andExpect(jsonPath("$.tags[1]").value("Log"));

        verify(momentService).updateMoment(eq(1L), any(UpdateMomentRequest.class));
    }

    @Test
    @WithMockUser
    void updateMoment_withNonExistentId_shouldReturnNotFound() throws Exception {
        when(momentService.updateMoment(eq(999L), any(UpdateMomentRequest.class)))
            .thenThrow(new TodoNotFoundException("Moment not found with id: 999"));

        mockMvc.perform(put("/api/v1/moments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateMomentRequest)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Moment not found with id: 999"));

        verify(momentService).updateMoment(eq(999L), any(UpdateMomentRequest.class));
    }

    @Test
    @WithMockUser
    void deleteMoment_withValidId_shouldReturnNoContent() throws Exception {
        doNothing().when(momentService).deleteMoment(1L);

        mockMvc.perform(delete("/api/v1/moments/1"))
            .andExpect(status().isNoContent());

        verify(momentService).deleteMoment(1L);
    }

    @Test
    @WithMockUser
    void deleteMoment_withNonExistentId_shouldReturnNotFound() throws Exception {
        doThrow(new TodoNotFoundException("Moment not found with id: 999"))
            .when(momentService).deleteMoment(999L);

        mockMvc.perform(delete("/api/v1/moments/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TODO_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Moment not found with id: 999"));

        verify(momentService).deleteMoment(999L);
    }

}