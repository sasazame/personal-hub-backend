package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestcontainersConfiguration.class)
class JwksControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getJwks_ShouldReturnValidJwksResponse() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].kty").exists())
                .andExpect(jsonPath("$.keys[0].use").exists())
                .andExpect(jsonPath("$.keys[0].kid").exists())
                .andExpect(jsonPath("$.keys[0].alg").exists());
    }

    @Test
    void getJwks_ShouldReturnRSAKeyWithRequiredFields() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].use").value("sig"))
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].e").exists());
    }

    @Test
    void getJwks_ShouldReturnConsistentKeyId() throws Exception {
        String response1 = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Verify that the key ID remains consistent across requests
        org.assertj.core.api.Assertions.assertThat(response1).isEqualTo(response2);
    }

    @Test
    void getJwks_ShouldReturnPublicKeyOnly() throws Exception {
        // Ensure no private key components are exposed
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].d").doesNotExist())
                .andExpect(jsonPath("$.keys[0].p").doesNotExist())
                .andExpect(jsonPath("$.keys[0].q").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dp").doesNotExist())
                .andExpect(jsonPath("$.keys[0].dq").doesNotExist())
                .andExpect(jsonPath("$.keys[0].qi").doesNotExist());
    }

    @Test
    void getJwks_ShouldBeAccessibleWithoutAuthentication() throws Exception {
        // JWKS endpoint should be publicly accessible
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk());
    }

    @Test
    void getJwks_ShouldReturnNotAcceptableForInvalidAcceptHeader() throws Exception {
        // When client requests XML but endpoint only produces JSON, it should return 406
        mockMvc.perform(get("/.well-known/jwks.json")
                .accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }
}