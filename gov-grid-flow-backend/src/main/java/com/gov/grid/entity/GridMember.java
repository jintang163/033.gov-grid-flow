package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("grid_member")
public class GridMember extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("grid_id")
    private Long gridId;

    @TableField("user_id")
    private Long userId;

    @TableField("member_type")
    private String memberType;
}
