package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("username")
    private String username;

    @TableField("password")
    private String password;

    @TableField("real_name")
    private String realName;

    @TableField("phone")
    private String phone;

    @TableField("email")
    private String email;

    @TableField("avatar")
    private String avatar;

    @TableField("status")
    private Integer status;

    @TableField("role")
    private String role;

    @TableField("dept_id")
    private Long deptId;

    @TableField("grid_id")
    private Long gridId;

    @TableField("openid")
    private String openid;
}
