package com.gov.grid.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String eventId;

    private Long userId;

    private String username;

    private String userRole;

    private String operation;

    private String module;

    private String description;

    private String method;

    private String requestUri;

    private String ipAddress;

    private String requestParams;

    private String responseResult;

    private Integer status;

    private String errorMsg;

    private Long costTime;

    private String currentHash;

    private Boolean tamperChecked;

    private LocalDateTime createdAt;
}
