package com.gov.grid.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarningInfoVO {

    private Integer urgeLevel;

    private String urgeLevelDesc;

    private Double remainingHours;

    private LocalDateTime deadlineAt;

    private Integer timeLimitHours;

    private Boolean isWarning;

    private Boolean isOverdue;

    private Double progressPercent;
}
