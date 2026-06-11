package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("resource_emergency")
public class ResourceEmergency extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("resource_code")
    private String resourceCode;

    @TableField("resource_name")
    private String resourceName;

    @TableField("resource_type")
    private String resourceType;

    @TableField("quantity")
    private Integer quantity;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("address")
    private String address;

    @TableField("grid_id")
    private Long gridId;

    @TableField("manager")
    private String manager;

    @TableField("manager_phone")
    private String managerPhone;

    @TableField("expire_date")
    private LocalDate expireDate;

    @TableField("status")
    private Integer status;

    @TableField("remark")
    private String remark;
}
