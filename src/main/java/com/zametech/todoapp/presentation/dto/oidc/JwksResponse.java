package com.zametech.todoapp.presentation.dto.oidc;

import lombok.Builder;

import java.util.List;

@Builder
public record JwksResponse(
    List<JwkKey> keys
) {
    @Builder
    public record JwkKey(
        String kty,
        String use,
        String keyOps,
        String alg,
        String kid,
        String x5u,
        List<String> x5c,
        String x5t,
        String x5tS256,
        String n,
        String e,
        String d,
        String p,
        String q,
        String dp,
        String dq,
        String qi,
        List<String> oth,
        String k,
        String x,
        String y,
        String crv
    ) {}
}