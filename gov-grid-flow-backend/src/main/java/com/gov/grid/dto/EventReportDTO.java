package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EventReportDTO {

    @NotBlank(message = "事件标题不能为空")
    private String title;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    private String description;

    private BigDecimal lng;

    private BigDecimal lat;

    private String address;

    private List<String> images;

    private List<String> videos;

    private String voiceUrl;

    @NotNull(message = "是否匿名不能为空")
    private Integer anonymous;

    private String reporterName;

    private String reporterPhone;

    private Long gridId;

    private String priority;
}
