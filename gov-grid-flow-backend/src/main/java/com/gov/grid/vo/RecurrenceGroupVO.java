package com.gov.grid.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecurrenceGroupVO {

    private String groupKey;

    private String eventType;

    private String eventTypeName;

    private Double lng;

    private Double lat;

    private String address;

    private Integer totalCount;

    private Integer pendingCount;

    private Integer completedCount;

    private Integer overdueCount;

    private LocalDateTime firstOccurAt;

    private LocalDateTime lastOccurAt;

    private List<EventSimpleVO> events;
}
