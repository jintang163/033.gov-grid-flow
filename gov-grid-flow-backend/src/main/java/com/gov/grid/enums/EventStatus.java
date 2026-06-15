package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum EventStatus {

    PENDING("PENDING", "待受理"),
    APPROVED("APPROVED", "已受理"),
    DISPATCHED("DISPATCHED", "已分派"),
    HANDLED("HANDLED", "已处置"),
    COMPLETED("COMPLETED", "已办结"),
    REJECTED("REJECTED", "已驳回"),
    TRANSFERRING("TRANSFERRING", "流转审批中"),
    TRANSFERRED("TRANSFERRED", "已跨街道转派");

    private final String code;
    private final String name;

    EventStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static EventStatus fromCode(String code) {
        for (EventStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知事件状态: " + code);
    }

    public static String getNameByCode(String code) {
        for (EventStatus status : values()) {
            if (status.code.equals(code)) {
                return status.name;
            }
        }
        return code;
    }
}
