package com.zametech.todoapp.presentation.dto.response;

public record NoteStatsResponse(
        long totalNotes,
        long notesThisWeek,
        long notesThisMonth,
        long totalTags
) {
}