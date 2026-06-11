package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum EventPriority {

    LOW("LOW", "低"),
    NORMAL("NORMAL", "一般"),
    HIGH("HIGH", "紧急"),
    URGENT("URGENT", "特急");

    private final String code;
    private final String name;

    EventPriority(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static EventPriority fromCode(String code) {
        for (EventPriority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        throw new IllegalArgumentException("未知事件优先级: " + code);
    }

    public static String getNameByCode(String code) {
        for (EventPriority priority : values()) {
            if (priority.code.equals(code)) {
                return priority.name;
            }
        }
        return code;
    }
}
