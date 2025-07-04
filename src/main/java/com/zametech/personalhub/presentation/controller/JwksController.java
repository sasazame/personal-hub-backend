package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.JwksService;
import com.zametech.personalhub.presentation.dto.oidc.JwksResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class JwksController {
    
    private final JwksService jwksService;
    
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public JwksResponse getJwks() {
        return jwksService.getJwks();
    }
}