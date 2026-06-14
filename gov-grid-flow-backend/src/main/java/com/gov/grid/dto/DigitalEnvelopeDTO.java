package com.gov.grid.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DigitalEnvelopeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String encryptedData;

    private String encryptedAesKey;

    private String fileUrl;

    private Long eventId;
}
