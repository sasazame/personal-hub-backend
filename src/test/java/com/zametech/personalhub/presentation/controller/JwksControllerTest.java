package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.JwksService;
import com.zametech.personalhub.presentation.dto.oidc.JwksResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = JwksController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@Import(TestSecurityConfig.class)
class JwksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwksService jwksService;

    private JwksResponse jwksResponse;

    @BeforeEach
    void setUp() {
        JwksResponse.JwkKey jwkKey = JwksResponse.JwkKey.builder()
            .kty("RSA")
            .use("sig")
            .alg("RS256")
            .kid("test-key-id")
            .n("modulus-value")
            .e("AQAB")
            .build();

        jwksResponse = JwksResponse.builder()
            .keys(List.of(jwkKey))
            .build();
    }

    @Test
    void getJwks_ShouldReturnJwksDocument() throws Exception {
        when(jwksService.getJwks()).thenReturn(jwksResponse);

        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
            .andExpect(jsonPath("$.keys[0].kid").value("test-key-id"))
            .andExpect(jsonPath("$.keys[0].n").value("modulus-value"))
            .andExpect(jsonPath("$.keys[0].e").value("AQAB"));
    }

    @Test
    void getJwks_WithMultipleKeys_ShouldReturnAll() throws Exception {
        JwksResponse.JwkKey jwkKey1 = JwksResponse.JwkKey.builder()
            .kty("RSA")
            .use("sig")
            .alg("RS256")
            .kid("key-1")
            .n("modulus-1")
            .e("AQAB")
            .build();

        JwksResponse.JwkKey jwkKey2 = JwksResponse.JwkKey.builder()
            .kty("RSA")
            .use("enc")
            .alg("RSA-OAEP")
            .kid("key-2")
            .n("modulus-2")
            .e("AQAB")
            .build();

        JwksResponse multiKeyResponse = JwksResponse.builder()
            .keys(List.of(jwkKey1, jwkKey2))
            .build();

        when(jwksService.getJwks()).thenReturn(multiKeyResponse);

        mockMvc.perform(get("/.well-known/jwks.json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys").isArray())
            .andExpect(jsonPath("$.keys[0].kid").value("key-1"))
            .andExpect(jsonPath("$.keys[0].use").value("sig"))
            .andExpect(jsonPath("$.keys[1].kid").value("key-2"))
            .andExpect(jsonPath("$.keys[1].use").value("enc"));
    }
}