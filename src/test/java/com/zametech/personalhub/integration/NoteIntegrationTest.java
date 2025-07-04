package com.zametech.personalhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zametech.personalhub.application.service.AuthenticationService;
import com.zametech.personalhub.presentation.dto.request.CreateNoteRequest;
import com.zametech.personalhub.presentation.dto.request.RegisterRequest;
import com.zametech.personalhub.presentation.dto.request.UpdateNoteRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.zametech.personalhub.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NoteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        try {
            // Create test user for integration tests
            RegisterRequest registerRequest = new RegisterRequest(
                    "test@example.com",
                    "TestPassword123!",
                    "testuser"
            );
            authenticationService.register(registerRequest);
        } catch (Exception e) {
            // User already exists, ignore
        }
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createNote_Success() throws Exception {
        CreateNoteRequest request = new CreateNoteRequest(
                "Meeting Notes",
                "Important discussion points from today's meeting",
                List.of("meeting", "important", "work")
        );

        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Meeting Notes"))
                .andExpect(jsonPath("$.content").value("Important discussion points from today's meeting"))
                .andExpect(jsonPath("$.tags").isArray())
                .andExpect(jsonPath("$.tags[0]").value("meeting"))
                .andExpect(jsonPath("$.tags[1]").value("important"))
                .andExpect(jsonPath("$.tags[2]").value("work"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void getNote_Success() throws Exception {
        // First create a note
        CreateNoteRequest createRequest = new CreateNoteRequest(
                "Test Note",
                "Test Content",
                List.of("test")
        );

        String createResponse = mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then get the note
        mockMvc.perform(get("/api/v1/notes/{id}", noteId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.content").value("Test Content"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void updateNote_Success() throws Exception {
        // First create a note
        CreateNoteRequest createRequest = new CreateNoteRequest(
                "Original Title",
                "Original Content",
                List.of("original")
        );

        String createResponse = mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then update the note
        UpdateNoteRequest updateRequest = new UpdateNoteRequest(
                "Updated Title",
                "Updated Content",
                List.of("updated", "new")
        );

        mockMvc.perform(put("/api/v1/notes/{id}", noteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(noteId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.tags[0]").value("updated"))
                .andExpect(jsonPath("$.tags[1]").value("new"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void searchNotes_Success() throws Exception {
        // First create a note with searchable content
        CreateNoteRequest createRequest = new CreateNoteRequest(
                "Spring Boot Tutorial",
                "This is a comprehensive guide to Spring Boot framework",
                List.of("spring", "tutorial")
        );

        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        // Then search for notes
        mockMvc.perform(get("/api/v1/notes/search")
                        .param("query", "spring"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Spring Boot Tutorial"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void deleteNote_Success() throws Exception {
        // First create a note
        CreateNoteRequest createRequest = new CreateNoteRequest(
                "Note to Delete",
                "This note will be deleted",
                List.of("delete")
        );

        String createResponse = mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long noteId = objectMapper.readTree(createResponse).get("id").asLong();

        // Then delete the note
        mockMvc.perform(delete("/api/v1/notes/{id}", noteId))
                .andExpect(status().isNoContent());

        // Verify the note is deleted
        mockMvc.perform(get("/api/v1/notes/{id}", noteId))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void createNote_InvalidData() throws Exception {
        CreateNoteRequest request = new CreateNoteRequest(
                "", // Empty title
                null,
                null
        );

        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
