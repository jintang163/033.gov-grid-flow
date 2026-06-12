package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum RoleEnum {

    ADMIN("admin", "系统管理员"),
    STREET_MANAGER("street_manager", "街道管理员"),
    GRID_LEADER("grid_leader", "网格长"),
    WORKER("worker", "网格员"),
    HANDLER("handler", "处置员"),
    SUPERVISOR("supervisor", "督查员");

    private final String code;
    private final String name;

    RoleEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static String getNameByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum roleEnum : values()) {
            if (roleEnum.getCode().equals(code)) {
                return roleEnum.getName();
            }
        }
        return null;
    }

    public static RoleEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (RoleEnum roleEnum : values()) {
            if (roleEnum.getCode().equals(code)) {
                return roleEnum;
            }
        }
        return null;
    }
}
