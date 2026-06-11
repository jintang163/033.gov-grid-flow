package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class EvaluationDTO {

    @NotNull(message = "事件ID不能为空")
    private Long eventId;

    @NotNull(message = "速度评分不能为空")
    @Min(value = 1, message = "速度评分最小为1")
    @Max(value = 5, message = "速度评分最大为5")
    private Integer speedScore;

    @NotNull(message = "效果评分不能为空")
    @Min(value = 1, message = "效果评分最小为1")
    @Max(value = 5, message = "效果评分最大为5")
    private Integer effectScore;

    private String content;
}
