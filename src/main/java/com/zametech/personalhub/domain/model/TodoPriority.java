package com.zametech.personalhub.domain.model;

/**
 * TODOの優先度
 */
public enum TodoPriority {
    /** 高 */
    HIGH("高", 1),
    /** 中 */
    MEDIUM("中", 2),
    /** 低 */
    LOW("低", 3);

    private final String displayName;
    private final int order;

    TodoPriority(String displayName, int order) {
        this.displayName = displayName;
        this.order = order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getOrder() {
        return order;
    }
}