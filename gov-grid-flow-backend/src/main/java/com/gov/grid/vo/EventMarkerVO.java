package com.gov.grid.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EventMarkerVO {

    private Long eventId;

    private String eventNo;

    private String title;

    private String eventType;

    private String status;

    private String priority;

    private BigDecimal lng;

    private BigDecimal lat;

    private String address;

    private String reporterName;

    private String reportTime;
}
