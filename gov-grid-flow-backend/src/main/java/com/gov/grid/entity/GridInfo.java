package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grid_info")
public class GridInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("grid_code")
    private String gridCode;

    @TableField("grid_name")
    private String gridName;

    @TableField("grid_leader_id")
    private Long gridLeaderId;

    @TableField("area")
    private BigDecimal area;

    @TableField("boundary")
    private String boundary;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("address")
    private String address;

    @TableField("status")
    private Integer status;
}
