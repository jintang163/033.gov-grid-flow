package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum UrgeLevelEnum {

    NONE(0, "未催办"),
    WARNING(1, "黄色预警"),
    OVERDUE(2, "红色超时"),
    ESCALATED(3, "升级督办");

    private final Integer code;
    private final String desc;

    UrgeLevelEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UrgeLevelEnum fromCode(Integer code) {
        if (code == null) {
            return NONE;
        }
        for (UrgeLevelEnum level : values()) {
            if (level.getCode().equals(code)) {
                return level;
            }
        }
        return NONE;
    }

    public static String getDescByCode(Integer code) {
        return fromCode(code).getDesc();
    }
}
