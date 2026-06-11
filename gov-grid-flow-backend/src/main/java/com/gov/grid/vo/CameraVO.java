package com.gov.grid.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CameraVO {
    private Long id;
    private String cameraCode;
    private String cameraName;
    private String cameraType;
    private String cameraTypeName;
    private BigDecimal lng;
    private BigDecimal lat;
    private String address;
    private String hlsUrl;
    private Integer status;
    private Double distance;
}
