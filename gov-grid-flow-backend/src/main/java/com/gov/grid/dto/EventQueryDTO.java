package com.gov.grid.dto;

import lombok.Data;

@Data
public class EventQueryDTO {

    private String keyword;

    private String status;

    private String eventType;

    private Long gridId;

    private String startTime;

    private String endTime;

    private Long reporterId;

    private Integer urgeLevel;

    private Integer pageNum;

    private Integer pageSize;

    private java.time.LocalDateTime createdAtStart;

    private java.time.LocalDateTime createdAtEnd;
}
