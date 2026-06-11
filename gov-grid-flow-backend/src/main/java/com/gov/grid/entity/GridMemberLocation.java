package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("grid_member_location")
public class GridMemberLocation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("phone")
    private String phone;

    @TableField("grid_id")
    private Long gridId;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("address")
    private String address;

    @TableField("on_duty")
    private Integer onDuty;

    @TableField("last_report_time")
    private LocalDateTime lastReportTime;

    @TableField("accuracy")
    private BigDecimal accuracy;

    @TableField("battery")
    private Integer battery;
}
