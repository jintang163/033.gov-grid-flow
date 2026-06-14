package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("file_watermark")
public class FileWatermark extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("file_url")
    private String fileUrl;

    @TableField("original_md5")
    private String originalMd5;

    @TableField("watermarked_md5")
    private String watermarkedMd5;

    @TableField("watermark_info")
    private String watermarkInfo;

    @TableField("event_id")
    private Long eventId;

    @TableField("reporter_id")
    private Long reporterId;

    @TableField("is_encrypted")
    private Integer isEncrypted;

    @TableField("encryption_key_id")
    private Long encryptionKeyId;

    @TableField("tamper_verified")
    private Integer tamperVerified;

    @TableField("tamper_verify_time")
    private LocalDateTime tamperVerifyTime;
}
