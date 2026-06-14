package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("encryption_key")
public class EncryptionKey extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("key_type")
    private String keyType;

    @TableField("key_name")
    private String keyName;

    @TableField("key_content")
    private String keyContent;

    @TableField("key_encrypted")
    private Integer keyEncrypted;

    @TableField("dept_id")
    private Long deptId;

    @TableField("status")
    private Integer status;
}
