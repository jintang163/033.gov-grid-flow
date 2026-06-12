package com.gov.grid.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ImageComparisonResultVO {

    private Long id;

    private Long eventId;

    private Long processId;

    private String beforeImage;

    private String afterImage;

    private BigDecimal similarity;

    private String heatmapImage;

    private String judgment;

    private String judgmentText;

    private String judgmentReason;

    private LocalDateTime createdAt;
}
