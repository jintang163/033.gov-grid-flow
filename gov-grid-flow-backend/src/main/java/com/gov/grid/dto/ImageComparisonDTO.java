package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ImageComparisonDTO {

    @NotNull(message = "事件ID不能为空")
    private Long eventId;

    private Long processId;

    @NotBlank(message = "处置前图片不能为空")
    private String beforeImage;

    @NotBlank(message = "处置后图片不能为空")
    private String afterImage;

    private String language = "zh";
}
