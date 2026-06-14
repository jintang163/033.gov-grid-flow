package com.gov.grid.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TamperCheckVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileWatermarkId;

    private String fileUrl;

    private Boolean tampered;

    private String originalMd5;

    private String currentMd5;

    private String watermarkInfo;

    private LocalDateTime verifyTime;

    private String message;
}
