package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_process")
public class EventProcess extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("task_id")
    private String taskId;

    @TableField("node_name")
    private String nodeName;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handler_name")
    private String handlerName;

    @TableField("action")
    private String action;

    @TableField("comment")
    private String comment;

    @TableField("attachments")
    private String attachments;

    @TableField("handle_time")
    private LocalDateTime handleTime;

    @TableField("duration_seconds")
    private Long durationSeconds;
}
