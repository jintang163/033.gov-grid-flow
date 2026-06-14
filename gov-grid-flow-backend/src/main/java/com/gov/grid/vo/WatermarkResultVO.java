package com.gov.grid.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class WatermarkResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fileUrl;

    private String originalMd5;

    private String watermarkedMd5;

    private String watermarkInfo;

    private Boolean tampered;

    private String tamperMessage;

    private Boolean watermarkApplied;

    private Boolean encrypted;
}
