package com.zametech.personalhub.presentation.dto.request;

import java.util.List;

public record UpdateMomentRequest(
        String content,
        List<String> tags
) {
}