package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_info")
public class EventInfo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_no")
    private String eventNo;

    @TableField("title")
    private String title;

    @TableField("event_type")
    private String eventType;

    @TableField("description")
    private String description;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("address")
    private String address;

    @TableField("images")
    private String images;

    @TableField("videos")
    private String videos;

    @TableField("anonymous")
    private Integer anonymous;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("reporter_name")
    private String reporterName;

    @TableField("reporter_phone")
    private String reporterPhone;

    @TableField("grid_id")
    private Long gridId;

    @TableField("status")
    private String status;

    @TableField("priority")
    private String priority;

    @TableField("process_instance_id")
    private String processInstanceId;
}
