package com.gov.grid.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class BatchSyncRequestDTO {

    @NotEmpty(message = "同步事件列表不能为空")
    @Valid
    private List<EventReportDTO> events;

    private Long timestamp;

    private String deviceId;
}
