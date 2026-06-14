package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_dispatch_record")
public class EventDispatchRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("recommended_dept_code")
    private String recommendedDeptCode;

    @TableField("recommended_dept_name")
    private String recommendedDeptName;

    @TableField("confidence")
    private BigDecimal confidence;

    @TableField("auto_dispatch")
    private Integer autoDispatch;

    @TableField("dispatch_method")
    private String dispatchMethod;

    @TableField("actual_dept_code")
    private String actualDeptCode;

    @TableField("actual_dept_name")
    private String actualDeptName;

    @TableField("status")
    private String status;

    @TableField("adopted")
    private Integer adopted;

    @TableField("feedback")
    private String feedback;

    @TableField("model_scores")
    private String modelScores;
}
