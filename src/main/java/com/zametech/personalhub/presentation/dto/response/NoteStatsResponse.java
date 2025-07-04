package com.zametech.personalhub.presentation.dto.response;

public record NoteStatsResponse(
        long totalNotes,
        long notesThisWeek,
        long notesThisMonth,
        long totalTags
) {
}