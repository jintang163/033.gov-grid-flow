package com.gov.grid.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AuditLogQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long userId;

    private String username;

    private String module;

    private String operation;

    private String keyword;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
