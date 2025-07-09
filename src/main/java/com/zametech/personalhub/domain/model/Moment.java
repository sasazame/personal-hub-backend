package com.zametech.personalhub.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Moment {
    public static final String TAG_IDEAS = "Ideas";
    public static final String TAG_DISCOVERIES = "Discoveries";
    public static final String TAG_EMOTIONS = "Emotions";
    public static final String TAG_LOG = "Log";
    public static final String TAG_OTHER = "Other";
    
    public static final List<String> DEFAULT_TAGS = List.of(
        TAG_IDEAS,
        TAG_DISCOVERIES,
        TAG_EMOTIONS,
        TAG_LOG,
        TAG_OTHER
    );
    
    private Long id;
    private String content;
    private String tags;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
    
    public boolean hasTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        return getTagList().stream()
                .anyMatch(t -> t.equalsIgnoreCase(tag.trim()));
    }
}