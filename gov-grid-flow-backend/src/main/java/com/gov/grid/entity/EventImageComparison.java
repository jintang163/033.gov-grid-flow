package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_image_comparison")
public class EventImageComparison extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("process_id")
    private Long processId;

    @TableField("before_image")
    private String beforeImage;

    @TableField("after_image")
    private String afterImage;

    @TableField("similarity")
    private BigDecimal similarity;

    @TableField("heatmap_image")
    private String heatmapImage;

    @TableField("judgment")
    private String judgment;

    @TableField("judgment_reason")
    private String judgmentReason;
}
