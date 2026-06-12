package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum GridLevelEnum {

    STREET(1, "街道"),
    COMMUNITY(2, "社区"),
    GRID(3, "网格"),
    MICRO_GRID(4, "微网格");

    private final Integer code;
    private final String name;

    GridLevelEnum(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GridLevelEnum levelEnum : values()) {
            if (levelEnum.getCode().equals(code)) {
                return levelEnum.getName();
            }
        }
        return null;
    }

    public static GridLevelEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (GridLevelEnum levelEnum : values()) {
            if (levelEnum.getCode().equals(code)) {
                return levelEnum;
            }
        }
        return null;
    }
}
