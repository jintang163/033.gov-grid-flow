package com.gov.grid.enums;

import lombok.Getter;

@Getter
public enum CrossStreetTransferStatus {

    PENDING_APPROVAL("PENDING_APPROVAL", "待上级审批"),
    APPROVED("APPROVED", "已审批通过"),
    REJECTED("REJECTED", "已驳回"),
    TRANSFERRED("TRANSFERRED", "已转派"),
    ACCEPTED("ACCEPTED", "接收方已接受"),
    PROCESSING("PROCESSING", "协作处理中"),
    COMPLETED("COMPLETED", "协作完成");

    private final String code;
    private final String name;

    CrossStreetTransferStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CrossStreetTransferStatus fromCode(String code) {
        for (CrossStreetTransferStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知流转状态: " + code);
    }

    public static String getNameByCode(String code) {
        for (CrossStreetTransferStatus status : values()) {
            if (status.code.equals(code)) {
                return status.name;
            }
        }
        return code;
    }
}
