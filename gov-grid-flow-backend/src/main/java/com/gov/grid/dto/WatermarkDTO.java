package com.gov.grid.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class WatermarkDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private MultipartFile file;

    @NotBlank(message = "上报时间不能为空")
    private String reportTime;

    @NotBlank(message = "网格员姓名不能为空")
    private String reporterName;

    private String eventNo;

    private Long eventId;

    private Long reporterId;

    private Boolean sensitive = false;
}
