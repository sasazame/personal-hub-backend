package com.zametech.todoapp.domain.model;

/**
 * TODOのステータス
 */
public enum TodoStatus {
    /** 未着手 */
    TODO("未着手"),
    /** 進行中 */
    IN_PROGRESS("進行中"),
    /** 完了 */
    DONE("完了");

    private final String displayName;

    TodoStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}