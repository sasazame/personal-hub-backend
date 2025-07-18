package com.zametech.personalhub.presentation.controller;

import com.zametech.personalhub.application.service.OidcDiscoveryService;
import com.zametech.personalhub.presentation.dto.oidc.OidcDiscoveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OidcDiscoveryController {
    
    private final OidcDiscoveryService oidcDiscoveryService;
    
    @GetMapping(value = "/.well-known/openid-configuration", produces = MediaType.APPLICATION_JSON_VALUE)
    public OidcDiscoveryResponse getDiscoveryDocument() {
        return oidcDiscoveryService.getDiscoveryDocument();
    }
}