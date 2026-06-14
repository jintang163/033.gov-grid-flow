package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_urge_record")
public class EventUrgeRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("event_no")
    private String eventNo;

    @TableField("urge_level")
    private Integer urgeLevel;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("template_id")
    private Long templateId;

    @TableField("title")
    private String title;

    @TableField("content")
    private String content;

    @TableField("channel")
    private String channel;

    @TableField("receiver_id")
    private Long receiverId;

    @TableField("receiver_name")
    private String receiverName;

    @TableField("send_status")
    private Integer sendStatus;

    @TableField("error_msg")
    private String errorMsg;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_at")
    private java.time.LocalDateTime readAt;
}
