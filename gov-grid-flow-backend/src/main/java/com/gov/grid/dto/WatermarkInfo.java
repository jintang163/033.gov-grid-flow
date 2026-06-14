package com.gov.grid.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatermarkInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportTime;

    private String reporterName;

    private String eventNo;
}
