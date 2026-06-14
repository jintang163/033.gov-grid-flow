package com.gov.grid.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BlockchainEvidenceVO {

    private Long id;

    private Long eventId;

    private String evidenceNo;

    private String chainType;

    private String txHash;

    private Long blockHeight;

    private LocalDateTime blockTime;

    private String evidenceHash;

    private Integer imageCount;

    private List<String> imageHashes;

    private Integer videoCount;

    private List<String> videoHashes;

    private String voiceHash;

    private String gpsHash;

    private String titleHash;

    private String descHash;

    private String status;

    private String certificateUrl;

    private Integer verified;

    private LocalDateTime verifyTime;

    private String remark;

    private LocalDateTime createdAt;

    private BigDecimal lng;

    private BigDecimal lat;

    private String address;

    private String title;

    private String reporterName;

    private String qrCodeDataUrl;
}
