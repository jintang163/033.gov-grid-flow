package com.gov.grid.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmergencyVO {
    private Long id;
    private String resourceCode;
    private String resourceName;
    private String resourceType;
    private String resourceTypeName;
    private Integer quantity;
    private BigDecimal lng;
    private BigDecimal lat;
    private String address;
    private String manager;
    private String managerPhone;
    private Integer status;
    private Double distance;
}
