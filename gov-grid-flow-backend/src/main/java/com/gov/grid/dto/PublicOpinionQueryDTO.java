package com.gov.grid.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
public class PublicOpinionQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long gridId;

    private String startDate;

    private String endDate;

    private Integer trendDays = 7;

    private Integer wordCloudSize = 50;

    private Integer hotEventSize = 10;
}
