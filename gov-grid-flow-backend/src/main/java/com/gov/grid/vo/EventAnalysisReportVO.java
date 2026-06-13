package com.gov.grid.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventAnalysisReportVO {

    private Long reportId;

    private String reportNo;

    private LocalDateTime generatedAt;

    private Integer totalEvents;

    private Integer highRecurrenceCount;

    private Integer totalRecurrenceGroups;

    private String period;

    private List<RecurrenceGroupVO> topRecurrenceGroups;

    private String summary;

    private String suggestions;
}
