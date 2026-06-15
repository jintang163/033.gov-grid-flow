package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class CrossStreetTransferDTO {

    @NotNull(message = "事件ID不能为空")
    private Long eventId;

    @NotNull(message = "转派目标部门ID不能为空")
    private Long targetDeptId;

    @NotBlank(message = "转派目标类型不能为空")
    private String targetType;

    @NotBlank(message = "转派原因不能为空")
    private String transferReason;

    private String crossBoundaryDescription;

    private String impactRange;

    private String urgencyLevel;

    private String coordinationNote;

    private List<String> attachments;

    private Long applicantId;

    private String applicantName;

    private String traceId;
}
