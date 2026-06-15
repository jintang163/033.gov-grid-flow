package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CrossStreetTransferApproveDTO {

    @NotNull(message = "流转ID不能为空")
    private Long transferId;

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;

    private String approveComment;

    private Long approverId;

    private String approverName;
}
