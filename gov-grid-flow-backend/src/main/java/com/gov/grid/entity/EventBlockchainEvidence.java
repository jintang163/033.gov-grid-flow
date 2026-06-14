package com.gov.grid.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.grid.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("event_blockchain_evidence")
public class EventBlockchainEvidence extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("event_id")
    private Long eventId;

    @TableField("evidence_no")
    private String evidenceNo;

    @TableField("chain_type")
    private String chainType;

    @TableField("tx_hash")
    private String txHash;

    @TableField("block_height")
    private Long blockHeight;

    @TableField("block_time")
    private java.time.LocalDateTime blockTime;

    @TableField("evidence_hash")
    private String evidenceHash;

    @TableField("image_count")
    private Integer imageCount;

    @TableField("image_hashes")
    private String imageHashes;

    @TableField("video_count")
    private Integer videoCount;

    @TableField("video_hashes")
    private String videoHashes;

    @TableField("voice_hash")
    private String voiceHash;

    @TableField("gps_hash")
    private String gpsHash;

    @TableField("title_hash")
    private String titleHash;

    @TableField("desc_hash")
    private String descHash;

    @TableField("reporter_info")
    private String reporterInfo;

    @TableField("status")
    private String status;

    @TableField("certificate_url")
    private String certificateUrl;

    @TableField("verified")
    private Integer verified;

    @TableField("verify_time")
    private java.time.LocalDateTime verifyTime;

    @TableField("remark")
    private String remark;
}
