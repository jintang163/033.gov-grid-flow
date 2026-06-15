package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CrossStreetTransferProcessDTO {

    @NotNull(message = "流转ID不能为空")
    private Long transferId;

    private String action;

    private String processResult;

    private String processDescription;

    private List<String> attachments;

    private Long operatorId;

    private String operatorName;

    private String comment;
}
