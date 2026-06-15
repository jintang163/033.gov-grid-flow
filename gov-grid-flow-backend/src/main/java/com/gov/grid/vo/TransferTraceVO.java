package com.gov.grid.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransferTraceVO {

    private Long id;

    private Long transferId;

    private String traceType;

    private String traceTypeName;

    private String nodeName;

    private Long operatorId;

    private String operatorName;

    private String operatorDeptName;

    private LocalDateTime operateTime;

    private String action;

    private String actionName;

    private String comment;

    private String fromDeptName;

    private String toDeptName;

    private String status;

    private String statusName;

    private String attachments;

    private String traceDetail;

    private Integer sortOrder;
}
