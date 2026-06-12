package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum ComparisonJudgment {

    PASS("PASS", "合格"),
    FAIL("FAIL", "不合格"),
    PENDING("PENDING", "待判定");

    private final String code;
    private final String name;

    ComparisonJudgment(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ComparisonJudgment fromCode(String code) {
        for (ComparisonJudgment value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return PENDING;
    }
}
