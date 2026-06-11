package com.gov.grid.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class LocationReportDTO {
    @NotNull(message = "经度不能为空")
    private BigDecimal lng;

    @NotNull(message = "纬度不能为空")
    private BigDecimal lat;

    private String address;

    private Integer onDuty;

    private BigDecimal accuracy;

    private Integer battery;
}
