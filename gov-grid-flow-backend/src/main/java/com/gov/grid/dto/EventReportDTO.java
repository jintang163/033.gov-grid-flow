package com.gov.grid.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;

@Data
public class EventReportDTO {

    private String clientId;

    private Long eventTimestamp;

    @NotBlank(message = "事件标题不能为空")
    private String title;

    @NotBlank(message = "事件类型不能为空")
    private String eventType;

    private String type;

    private String description;

    private BigDecimal lng;

    private BigDecimal lat;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String address;

    private List<String> images;

    private List<String> imageUrls;

    private List<String> videos;

    private String voiceUrl;

    private Integer anonymous = 0;

    private String reporterName;

    private String reporterPhone;

    private Long gridId;

    private String priority;

    private String reporterId;

    private Integer blockchainEnabled = 0;

    public String getEventTypeResolved() {
        if (eventType != null && !eventType.isEmpty()) {
            return eventType;
        }
        return type;
    }

    public BigDecimal getLngResolved() {
        if (lng != null) {
            return lng;
        }
        return longitude;
    }

    public BigDecimal getLatResolved() {
        if (lat != null) {
            return lat;
        }
        return latitude;
    }

    public List<String> getImagesResolved() {
        if (images != null && !images.isEmpty()) {
            return images;
        }
        return imageUrls;
    }
}
