package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_cross_street_transfer")
public class EventCrossStreetTransfer extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("event_no")
    private String eventNo;

    @TableField("event_title")
    private String eventTitle;

    @TableField("event_type")
    private String eventType;

    @TableField("source_dept_id")
    private Long sourceDeptId;

    @TableField("source_dept_name")
    private String sourceDeptName;

    @TableField("source_grid_id")
    private Long sourceGridId;

    @TableField("source_grid_name")
    private String sourceGridName;

    @TableField("target_dept_id")
    private Long targetDeptId;

    @TableField("target_dept_name")
    private String targetDeptName;

    @TableField("target_dept_code")
    private String targetDeptCode;

    @TableField("target_type")
    private String targetType;

    @TableField("transfer_reason")
    private String transferReason;

    @TableField("cross_boundary_description")
    private String crossBoundaryDescription;

    @TableField("impact_range")
    private String impactRange;

    @TableField("status")
    private String status;

    @TableField("applicant_id")
    private Long applicantId;

    @TableField("applicant_name")
    private String applicantName;

    @TableField("applicant_time")
    private LocalDateTime applicantTime;

    @TableField("approver_id")
    private Long approverId;

    @TableField("approver_name")
    private String approverName;

    @TableField("approve_time")
    private LocalDateTime approveTime;

    @TableField("approve_comment")
    private String approveComment;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("receive_time")
    private LocalDateTime receiveTime;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handler_name")
    private String handlerName;

    @TableField("process_start_time")
    private LocalDateTime processStartTime;

    @TableField("process_end_time")
    private LocalDateTime processEndTime;

    @TableField("process_result")
    private String processResult;

    @TableField("process_description")
    private String processDescription;

    @TableField("lng")
    private BigDecimal lng;

    @TableField("lat")
    private BigDecimal lat;

    @TableField("address")
    private String address;

    @TableField("urgency_level")
    private String urgencyLevel;

    @TableField("attachments")
    private String attachments;

    @TableField("coordination_note")
    private String coordinationNote;

    @TableField("trace_id")
    private String traceId;

    @TableField(exist = false)
    private Integer overdueDays;

    @TableField(exist = false)
    private String currentNodeName;
}
