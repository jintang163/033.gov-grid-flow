package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum TransferTargetType {

    STREET("STREET", "相邻街道"),
    BUREAU("BUREAU", "委办局"),
    COUNTY("COUNTY", "区级部门");

    private final String code;
    private final String name;

    TransferTargetType(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static TransferTargetType fromCode(String code) {
        for (TransferTargetType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知转派目标类型: " + code);
    }

    public static String getNameByCode(String code) {
        for (TransferTargetType type : values()) {
            if (type.code.equals(code)) {
                return type.name;
            }
        }
        return code;
    }
}
