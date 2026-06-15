package com.gov.grid.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CrossStreetTransferVO {

    private Long id;

    private Long eventId;

    private String eventNo;

    private String eventTitle;

    private String eventType;

    private String eventTypeName;

    private Long sourceDeptId;

    private String sourceDeptName;

    private Long sourceGridId;

    private String sourceGridName;

    private Long targetDeptId;

    private String targetDeptName;

    private String targetDeptCode;

    private String targetType;

    private String targetTypeName;

    private String transferReason;

    private String crossBoundaryDescription;

    private String impactRange;

    private String status;

    private String statusName;

    private String statusTagType;

    private Long applicantId;

    private String applicantName;

    private LocalDateTime applicantTime;

    private Long approverId;

    private String approverName;

    private LocalDateTime approveTime;

    private String approveComment;

    private Long receiverId;

    private String receiverName;

    private LocalDateTime receiveTime;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime processStartTime;

    private LocalDateTime processEndTime;

    private String processResult;

    private String processDescription;

    private String lng;

    private String lat;

    private String address;

    private String urgencyLevel;

    private String urgencyLevelName;

    private List<String> attachmentList;

    private String coordinationNote;

    private String traceId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Boolean canApprove;

    private Boolean canReceive;

    private Boolean canProcess;

    private Boolean canComplete;

    private List<TransferTraceVO> traceList;
}
