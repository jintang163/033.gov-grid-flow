package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum FlowableCandidateGroup {

    GRID_LEADER("grid_leader", "网格长"),
    ADMIN("admin", "管理员"),
    HANDLER("handler", "处置员"),
    WORKER("worker", "网格员");

    private final String code;
    private final String name;

    FlowableCandidateGroup(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static FlowableCandidateGroup fromCode(String code) {
        for (FlowableCandidateGroup group : values()) {
            if (group.code.equals(code)) {
                return group;
            }
        }
        throw new IllegalArgumentException("未知候选组: " + code);
    }
}
