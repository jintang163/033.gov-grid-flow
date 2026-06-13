package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_urge_template")
public class EventUrgeTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("template_code")
    private String templateCode;

    @TableField("template_name")
    private String templateName;

    @TableField("title_template")
    private String titleTemplate;

    @TableField("content_template")
    private String contentTemplate;

    @TableField("channel")
    private String channel;

    @TableField("enabled")
    private Integer enabled;
}
