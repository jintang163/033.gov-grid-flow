package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
public class EventProcessDTO {

    @NotNull(message = "事件ID不能为空")
    private Long eventId;

    private Long taskId;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private String comment;

    private List<String> attachments;

    private List<String> afterImages;

    private Long assignee;
}
