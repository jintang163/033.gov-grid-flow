package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_evaluation")
public class EventEvaluation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("speed_score")
    private Integer speedScore;

    @TableField("effect_score")
    private Integer effectScore;

    @TableField("content")
    private String content;
}
