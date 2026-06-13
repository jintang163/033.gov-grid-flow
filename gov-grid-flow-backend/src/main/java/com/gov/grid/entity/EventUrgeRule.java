package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_urge_rule")
public class EventUrgeRule extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_type")
    private String eventType;

    @TableField("event_type_name")
    private String eventTypeName;

    @TableField("time_limit_hours")
    private Integer timeLimitHours;

    @TableField("warning_ratio")
    private BigDecimal warningRatio;

    @TableField("escalate_level")
    private String escalateLevel;

    @TableField("enabled")
    private Integer enabled;
}
