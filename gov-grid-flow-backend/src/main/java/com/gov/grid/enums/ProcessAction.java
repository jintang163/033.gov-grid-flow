package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum ProcessAction {

    SUBMIT("SUBMIT", "事件上报"),
    APPROVE("APPROVE", "审核受理"),
    DISPATCH("DISPATCH", "任务分派"),
    ASSIGN("ASSIGN", "指派处置"),
    HANDLE("HANDLE", "事件处置"),
    VERIFY("VERIFY", "核查结案"),
    REJECT("REJECT", "退回");

    private final String code;
    private final String name;

    ProcessAction(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ProcessAction fromCode(String code) {
        for (ProcessAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        throw new IllegalArgumentException("未知操作类型: " + code);
    }

    public static String getNameByCode(String code) {
        for (ProcessAction action : values()) {
            if (action.code.equals(code)) {
                return action.name;
            }
        }
        return code;
    }
}
