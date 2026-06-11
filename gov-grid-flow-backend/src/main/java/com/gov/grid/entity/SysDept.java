package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("parent_id")
    private Long parentId;

    @TableField("leader")
    private String leader;

    @TableField("phone")
    private String phone;

    @TableField("sort")
    private Integer sort;

    @TableField("status")
    private Integer status;
}
