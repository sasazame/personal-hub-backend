package com.zametech.personalhub.presentation.dto.request;

import java.util.List;

public record UpdateNoteRequest(
        String title,
        String content,
        List<String> tags
) {
}