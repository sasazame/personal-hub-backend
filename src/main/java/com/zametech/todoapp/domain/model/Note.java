package com.zametech.todoapp.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Note {
    private Long id;
    private String title;
    private String content;
    private String tags;
    private Long userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTags(String tags) {
        this.tags = tags;
        this.updatedAt = LocalDateTime.now();
    }

    public void setTags(List<String> tagList) {
        this.tags = tagList != null ? String.join(",", tagList) : null;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getTagList() {
        if (tags == null || tags.trim().isEmpty()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .toList();
    }
}