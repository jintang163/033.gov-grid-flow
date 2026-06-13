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
        if (code == null) {
            return NORMAL;
        }
        String normalized = code.trim().toUpperCase();
        switch (normalized) {
            case "LOW":
            case "LOW_PRIORITY":
                return LOW;
            case "MEDIUM":
            case "NORMAL":
                return NORMAL;
            case "HIGH":
            case "HIGH_PRIORITY":
                return HIGH;
            case "URGENT":
            case "CRITICAL":
                return URGENT;
            default:
                for (EventPriority priority : values()) {
                    if (priority.code.equalsIgnoreCase(code)) {
                        return priority;
                    }
                }
                return NORMAL;
        }
    }

    public static String normalize(String code) {
        return fromCode(code).getCode();
    }

    public static String getNameByCode(String code) {
        return fromCode(code).getName();
    }
}
