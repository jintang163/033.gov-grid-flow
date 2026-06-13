package com.gov.grid.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EventSimpleVO {

    private Long id;

    private String eventNo;

    private String title;

    private String status;

    private String statusName;

    private String priority;

    private String priorityName;

    private LocalDateTime createdAt;

    private LocalDateTime deadlineAt;

    private Integer urgeLevel;

    private String reporterName;

    private String handlerName;
}
