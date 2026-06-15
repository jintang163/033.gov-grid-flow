package com.gov.grid.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.grid.common.PageResult;
import com.gov.grid.common.exception.BusinessException;
import com.gov.grid.dto.CrossStreetTransferApproveDTO;
import com.gov.grid.dto.CrossStreetTransferDTO;
import com.gov.grid.dto.CrossStreetTransferProcessDTO;
import com.gov.grid.entity.EventCrossStreetTransfer;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.entity.GridInfo;
import com.gov.grid.entity.SysDept;
import com.gov.grid.entity.SysUser;
import com.gov.grid.enums.CrossStreetTransferStatus;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.TransferTargetType;
import com.gov.grid.mapper.EventCrossStreetTransferMapper;
import com.gov.grid.mapper.EventInfoMapper;
import com.gov.grid.mapper.EventProcessMapper;
import com.gov.grid.mapper.GridInfoMapper;
import com.gov.grid.mapper.SysDeptMapper;
import com.gov.grid.mapper.SysUserMapper;
import com.gov.grid.security.DataScopeUtils;
import com.gov.grid.service.CrossStreetTransferService;
import com.gov.grid.service.GridService;
import com.gov.grid.service.NotificationService;
import com.gov.grid.entity.EventProcess;
import com.gov.grid.enums.ProcessAction;
import com.gov.grid.enums.RoleEnum;
import com.gov.grid.vo.CrossStreetTransferVO;
import com.gov.grid.vo.DeptTreeVO;
import com.gov.grid.vo.TransferTraceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrossStreetTransferServiceImpl implements CrossStreetTransferService {

    private final EventCrossStreetTransferMapper transferMapper;
    private final EventInfoMapper eventInfoMapper;
    private final EventProcessMapper eventProcessMapper;
    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final GridInfoMapper gridInfoMapper;
    private final GridService gridService;
    private final NotificationService notificationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyTransfer(CrossStreetTransferDTO dto, Long userId) {
        EventInfo eventInfo = eventInfoMapper.selectById(dto.getEventId());
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!DataScopeUtils.canAccessGrid(eventInfo.getGridId())) {
            throw new BusinessException("无权操作该事件");
        }

        validateTransferAuthority(eventInfo, user);

        if (StrUtil.isBlank(dto.getTransferReason())) {
            throw new BusinessException("请填写转派原因");
        }
        if (StrUtil.isBlank(dto.getCrossBoundaryDescription())) {
            throw new BusinessException("请填写跨界描述");
        }
        if (StrUtil.isBlank(dto.getImpactRange())) {
            throw new BusinessException("请填写影响范围");
        }

        LambdaQueryWrapper<EventCrossStreetTransfer> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(EventCrossStreetTransfer::getEventId, dto.getEventId())
                .in(EventCrossStreetTransfer::getStatus, Arrays.asList(
                        CrossStreetTransferStatus.PENDING_APPROVAL.getCode(),
                        CrossStreetTransferStatus.APPROVED.getCode(),
                        CrossStreetTransferStatus.TRANSFERRED.getCode(),
                        CrossStreetTransferStatus.ACCEPTED.getCode(),
                        CrossStreetTransferStatus.PROCESSING.getCode()
                ));
        Long existingCount = transferMapper.selectCount(checkWrapper);
        if (existingCount != null && existingCount > 0) {
            throw new BusinessException("该事件已有进行中的跨街道流转申请");
        }

        SysDept targetDept = sysDeptMapper.selectById(dto.getTargetDeptId());
        if (targetDept == null) {
            throw new BusinessException("目标协作机构不存在");
        }

        SysDept sourceDept = sysDeptMapper.selectById(user.getDeptId());
        GridInfo sourceGrid = gridInfoMapper.selectById(eventInfo.getGridId());

        EventCrossStreetTransfer transfer = new EventCrossStreetTransfer();
        transfer.setEventId(dto.getEventId());
        transfer.setEventNo(eventInfo.getEventNo());
        transfer.setEventTitle(eventInfo.getTitle());
        transfer.setEventType(eventInfo.getEventType());
        transfer.setSourceDeptId(user.getDeptId());
        transfer.setSourceDeptName(sourceDept != null ? sourceDept.getName() : "");
        transfer.setSourceGridId(eventInfo.getGridId());
        transfer.setSourceGridName(sourceGrid != null ? sourceGrid.getGridName() : "");
        transfer.setTargetDeptId(dto.getTargetDeptId());
        transfer.setTargetDeptName(targetDept.getName());
        transfer.setTargetDeptCode(targetDept.getCode());
        transfer.setTargetType(dto.getTargetType());
        transfer.setTransferReason(dto.getTransferReason());
        transfer.setCrossBoundaryDescription(dto.getCrossBoundaryDescription());
        transfer.setImpactRange(dto.getImpactRange());
        transfer.setUrgencyLevel(dto.getUrgencyLevel() != null ? dto.getUrgencyLevel() : eventInfo.getPriority());
        transfer.setCoordinationNote(dto.getCoordinationNote());
        if (CollUtil.isNotEmpty(dto.getAttachments())) {
            transfer.setAttachments(String.join(",", dto.getAttachments()));
        }
        transfer.setLng(eventInfo.getLng());
        transfer.setLat(eventInfo.getLat());
        transfer.setAddress(eventInfo.getAddress());
        transfer.setStatus(CrossStreetTransferStatus.PENDING_APPROVAL.getCode());
        transfer.setApplicantId(userId);
        transfer.setApplicantName(user.getRealName());
        transfer.setApplicantTime(LocalDateTime.now());
        transfer.setTraceId(IdUtil.simpleUUID());

        transferMapper.insert(transfer);

        eventInfo.setStatus("TRANSFERRING");
        eventInfoMapper.updateById(eventInfo);

        addEventProcessRecord(eventInfo.getId(), ProcessAction.TRANSFER_APPLY.getCode(),
                "跨街流转申请", userId, user.getRealName(),
                "申请转至" + targetDept.getName() + "，原因：" + dto.getTransferReason(),
                null);

        sendTransferNotification(transfer, "PENDING_APPROVAL", userId);

        log.info("跨街道流转申请提交成功，事件ID：{}，流转ID：{}，申请人：{}",
                dto.getEventId(), transfer.getId(), userId);

        return transfer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approveTransfer(CrossStreetTransferApproveDTO dto, Long userId) {
        EventCrossStreetTransfer transfer = transferMapper.selectById(dto.getTransferId());
        if (transfer == null) {
            throw new BusinessException("流转记录不存在");
        }

        if (!CrossStreetTransferStatus.PENDING_APPROVAL.getCode().equals(transfer.getStatus())) {
            throw new BusinessException("当前状态不允许审批");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!canApproveTransfer(transfer, user)) {
            throw new BusinessException("您没有审批该流转申请的权限，请联系上级部门审批");
        }

        transfer.setApproverId(userId);
        transfer.setApproverName(user.getRealName());
        transfer.setApproveTime(LocalDateTime.now());
        transfer.setApproveComment(dto.getApproveComment());

        if (dto.getApproved()) {
            transfer.setStatus(CrossStreetTransferStatus.APPROVED.getCode());
            transferMapper.updateById(transfer);

            transfer.setStatus(CrossStreetTransferStatus.TRANSFERRED.getCode());
            transferMapper.updateById(transfer);

            EventInfo eventInfo = eventInfoMapper.selectById(transfer.getEventId());
            if (eventInfo != null) {
                eventInfo.setStatus("TRANSFERRED");
                eventInfoMapper.updateById(eventInfo);
            }

            addEventProcessRecord(transfer.getEventId(), ProcessAction.TRANSFER_APPROVE.getCode(),
                    "跨街流转审批通过", userId, user.getRealName(),
                    "审批通过，转至" + transfer.getTargetDeptName() + "。" +
                            (dto.getApproveComment() != null ? "审批意见：" + dto.getApproveComment() : ""),
                    null);

            sendTransferNotification(transfer, "TRANSFERRED", userId);
            log.info("跨街道流转审批通过，流转ID：{}，审批人：{}", dto.getTransferId(), userId);
        } else {
            transfer.setStatus(CrossStreetTransferStatus.REJECTED.getCode());
            transferMapper.updateById(transfer);

            EventInfo eventInfo = eventInfoMapper.selectById(transfer.getEventId());
            if (eventInfo != null) {
                eventInfo.setStatus(EventStatus.APPROVED.getCode());
                eventInfoMapper.updateById(eventInfo);
            }

            addEventProcessRecord(transfer.getEventId(), ProcessAction.TRANSFER_REJECT.getCode(),
                    "跨街流转申请驳回", userId, user.getRealName(),
                    "驳回原因：" + (dto.getApproveComment() != null ? dto.getApproveComment() : "暂无"),
                    null);

            sendTransferNotification(transfer, "REJECTED", userId);
            log.info("跨街道流转申请被驳回，流转ID：{}，审批人：{}", dto.getTransferId(), userId);
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean receiveTransfer(Long transferId, Long userId) {
        EventCrossStreetTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            throw new BusinessException("流转记录不存在");
        }

        if (!CrossStreetTransferStatus.TRANSFERRED.getCode().equals(transfer.getStatus())) {
            throw new BusinessException("当前状态不允许接收");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getDeptId() == null || !user.getDeptId().equals(transfer.getTargetDeptId())) {
            throw new BusinessException("您不属于接收方部门，无权接收");
        }

        transfer.setReceiverId(userId);
        transfer.setReceiverName(user.getRealName());
        transfer.setReceiveTime(LocalDateTime.now());
        transfer.setStatus(CrossStreetTransferStatus.ACCEPTED.getCode());
        transferMapper.updateById(transfer);

        transfer.setStatus(CrossStreetTransferStatus.PROCESSING.getCode());
        transfer.setHandlerId(userId);
        transfer.setHandlerName(user.getRealName());
        transfer.setProcessStartTime(LocalDateTime.now());
        transferMapper.updateById(transfer);

        EventInfo eventInfo = eventInfoMapper.selectById(transfer.getEventId());
        if (eventInfo != null) {
            eventInfo.setStatus(EventStatus.DISPATCHED.getCode());
            eventInfo.setGridId(eventInfo.getGridId());
            eventInfoMapper.updateById(eventInfo);
        }

        addEventProcessRecord(transfer.getEventId(), ProcessAction.TRANSFER_RECEIVE.getCode(),
                "接收跨街流转", userId, user.getRealName(),
                "已接收来自" + transfer.getSourceDeptName() + "的协作任务，开始处理",
                null);

        sendTransferNotification(transfer, "ACCEPTED", userId);

        log.info("跨街道流转已接收并开始处理，流转ID：{}，接收人：{}", transferId, userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processTransfer(CrossStreetTransferProcessDTO dto, Long userId) {
        EventCrossStreetTransfer transfer = transferMapper.selectById(dto.getTransferId());
        if (transfer == null) {
            throw new BusinessException("流转记录不存在");
        }

        if (!CrossStreetTransferStatus.PROCESSING.getCode().equals(transfer.getStatus())) {
            throw new BusinessException("当前状态不允许处理");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getDeptId() == null || !user.getDeptId().equals(transfer.getTargetDeptId())) {
            throw new BusinessException("您不属于接收方部门，无权处理");
        }

        if (StrUtil.isNotBlank(dto.getProcessDescription())) {
            transfer.setProcessDescription(transfer.getProcessDescription() == null
                    ? dto.getProcessDescription()
                    : transfer.getProcessDescription() + "；" + dto.getProcessDescription());
        }
        transfer.setUpdateTime(LocalDateTime.now());
        transferMapper.updateById(transfer);

        String attachments = CollUtil.isNotEmpty(dto.getAttachments())
                ? String.join(",", dto.getAttachments()) : null;
        addEventProcessRecord(transfer.getEventId(), ProcessAction.TRANSFER_PROCESS.getCode(),
                "跨街协作处理", userId, user.getRealName(),
                dto.getProcessDescription(),
                attachments);

        sendTransferNotification(transfer, "PROCESSING", userId);

        log.info("跨街道流转处理记录已添加，流转ID：{}，处理人：{}", dto.getTransferId(), userId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeTransfer(CrossStreetTransferProcessDTO dto, Long userId) {
        EventCrossStreetTransfer transfer = transferMapper.selectById(dto.getTransferId());
        if (transfer == null) {
            throw new BusinessException("流转记录不存在");
        }

        if (!CrossStreetTransferStatus.PROCESSING.getCode().equals(transfer.getStatus())) {
            throw new BusinessException("当前状态不允许办结");
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (user.getDeptId() == null || !user.getDeptId().equals(transfer.getTargetDeptId())) {
            throw new BusinessException("您不属于接收方部门，无权办结");
        }

        if (StrUtil.isBlank(dto.getProcessResult())) {
            throw new BusinessException("请填写处理结果");
        }

        transfer.setProcessResult(dto.getProcessResult());
        if (StrUtil.isNotBlank(dto.getProcessDescription())) {
            transfer.setProcessDescription(transfer.getProcessDescription() == null
                    ? dto.getProcessDescription()
                    : transfer.getProcessDescription() + "；" + dto.getProcessDescription());
        }
        if (CollUtil.isNotEmpty(dto.getAttachments())) {
            String newAttachments = String.join(",", dto.getAttachments());
            transfer.setAttachments(transfer.getAttachments() == null
                    ? newAttachments
                    : transfer.getAttachments() + "," + newAttachments);
        }
        transfer.setProcessEndTime(LocalDateTime.now());
        transfer.setStatus(CrossStreetTransferStatus.COMPLETED.getCode());
        transferMapper.updateById(transfer);

        EventInfo eventInfo = eventInfoMapper.selectById(transfer.getEventId());
        if (eventInfo != null) {
            eventInfo.setStatus(EventStatus.HANDLED.getCode());
            eventInfoMapper.updateById(eventInfo);
        }

        String comment = "处理结果：" + dto.getProcessResult();
        if (StrUtil.isNotBlank(dto.getProcessDescription())) {
            comment += "，处理详情：" + dto.getProcessDescription();
        }
        String attachments = CollUtil.isNotEmpty(dto.getAttachments())
                ? String.join(",", dto.getAttachments()) : null;
        addEventProcessRecord(transfer.getEventId(), ProcessAction.TRANSFER_COMPLETE.getCode(),
                "跨街流转办结", userId, user.getRealName(),
                comment,
                attachments);

        sendTransferNotification(transfer, "COMPLETED", userId);

        log.info("跨街道流转已办结，流转ID：{}，处理结果：{}", dto.getTransferId(), dto.getProcessResult());
        return true;
    }

    @Override
    public CrossStreetTransferVO getTransferDetail(Long transferId) {
        EventCrossStreetTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            throw new BusinessException("流转记录不存在");
        }
        return convertToVO(transfer);
    }

    @Override
    public PageResult<CrossStreetTransferVO> getTransferPage(
            Integer pageNum, Integer pageSize,
            Long eventId, Long sourceDeptId, Long targetDeptId,
            String status, String targetType, Long userId, String keyword) {

        Page<EventCrossStreetTransfer> page = new Page<>(pageNum, pageSize);
        List<EventCrossStreetTransfer> records = transferMapper.selectTransferList(
                eventId, sourceDeptId, targetDeptId, status, targetType, userId, keyword
        );

        int total = records.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<EventCrossStreetTransfer> pageRecords = start < total
                ? records.subList(start, end)
                : new ArrayList<>();

        List<CrossStreetTransferVO> voList = pageRecords.stream()
                .map(this::convertToSimpleVO)
                .collect(Collectors.toList());

        IPage<CrossStreetTransferVO> resultPage = new Page<>(pageNum, pageSize, total);
        resultPage.setRecords(voList);
        return PageResult.of(resultPage);
    }

    @Override
    public List<CrossStreetTransferVO> getMyInvolvedTransfers(Long userId, Long deptId, String status) {
        List<EventCrossStreetTransfer> records = transferMapper.selectMyInvolvedTransfers(userId, deptId, status);
        return records.stream()
                .map(this::convertToSimpleVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<CrossStreetTransferVO> getEventTransferHistory(Long eventId) {
        LambdaQueryWrapper<EventCrossStreetTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventCrossStreetTransfer::getEventId, eventId);
        wrapper.orderByDesc(EventCrossStreetTransfer::getApplicantTime);
        List<EventCrossStreetTransfer> records = transferMapper.selectList(wrapper);
        return records.stream()
                .map(this::convertToSimpleVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<TransferTraceVO> getTransferTrace(Long transferId) {
        EventCrossStreetTransfer transfer = transferMapper.selectById(transferId);
        if (transfer == null) {
            throw new BusinessException("流转记录不存在");
        }

        List<TransferTraceVO> traceList = new ArrayList<>();

        List<EventProcess> processRecords = getEventProcessRecords(transfer.getEventId());
        int sort = 0;
        for (EventProcess process : processRecords) {
            TransferTraceVO trace = buildTraceFromProcess(sort++, transferId, transfer, process);
            if (trace != null) {
                traceList.add(trace);
            }
        }

        if (traceList.isEmpty()) {
            traceList.add(buildTrace(sort++, transferId, "APPLY", "流转申请",
                    transfer.getApplicantId(), transfer.getApplicantName(),
                    transfer.getSourceDeptName(), null,
                    transfer.getApplicantTime(),
                    "提交跨街道流转申请，原因为：" + transfer.getTransferReason(),
                    transfer.getStatus()));

            if (transfer.getApproverId() != null) {
                String actionName = CrossStreetTransferStatus.REJECTED.getCode().equals(transfer.getStatus())
                        ? "驳回申请" : "审批通过";
                traceList.add(buildTrace(sort++, transferId, "APPROVE", "上级审批",
                        transfer.getApproverId(), transfer.getApproverName(),
                        null, transfer.getTargetDeptName(),
                        transfer.getApproveTime(),
                        actionName + "：" + (transfer.getApproveComment() != null ? transfer.getApproveComment() : ""),
                        transfer.getStatus()));
            }

            if (transfer.getReceiverId() != null) {
                traceList.add(buildTrace(sort++, transferId, "RECEIVE", "接收流转",
                        transfer.getReceiverId(), transfer.getReceiverName(),
                        null, null,
                        transfer.getReceiveTime(),
                        "已接收流转，开始协作处理",
                        CrossStreetTransferStatus.ACCEPTED.getCode()));
            }

            if (transfer.getProcessStartTime() != null) {
                traceList.add(buildTrace(sort++, transferId, "PROCESS_START", "开始处理",
                        transfer.getHandlerId(), transfer.getHandlerName(),
                        null, null,
                        transfer.getProcessStartTime(),
                        "协作处理中",
                        CrossStreetTransferStatus.PROCESSING.getCode()));
            }

            if (transfer.getProcessEndTime() != null) {
                traceList.add(buildTrace(sort++, transferId, "COMPLETE", "办结",
                        transfer.getHandlerId(), transfer.getHandlerName(),
                        null, null,
                        transfer.getProcessEndTime(),
                        "协作处理完成，结果：" + transfer.getProcessResult(),
                        CrossStreetTransferStatus.COMPLETED.getCode()));
            }
        }

        return traceList;
    }

    private List<EventProcess> getEventProcessRecords(Long eventId) {
        if (eventId == null) {
            return new ArrayList<>();
        }
        LambdaQueryWrapper<EventProcess> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EventProcess::getEventId, eventId);
        wrapper.orderByAsc(EventProcess::getHandleTime);
        return eventProcessMapper.selectList(wrapper);
    }

    private TransferTraceVO buildTraceFromProcess(int sort, Long transferId,
                                                  EventCrossStreetTransfer transfer, EventProcess process) {
        if (process == null || process.getAction() == null) {
            return null;
        }

        String action = process.getAction();
        String traceType = "PROCESS";
        String nodeName = ProcessAction.getNameByCode(action);
        if (nodeName == null || nodeName.equals(action)) {
            nodeName = process.getNodeName() != null ? process.getNodeName() : action;
        }

        String fromDeptName = null;
        String toDeptName = null;
        String status = null;

        boolean isTransferAction = action.startsWith("TRANSFER_");
        if (isTransferAction) {
            traceType = "TRANSFER";
        }

        if (ProcessAction.TRANSFER_APPLY.getCode().equals(action)) {
            fromDeptName = transfer.getSourceDeptName();
            status = CrossStreetTransferStatus.PENDING_APPROVAL.getCode();
        } else if (ProcessAction.TRANSFER_APPROVE.getCode().equals(action)) {
            toDeptName = transfer.getTargetDeptName();
            status = CrossStreetTransferStatus.TRANSFERRED.getCode();
        } else if (ProcessAction.TRANSFER_REJECT.getCode().equals(action)) {
            status = CrossStreetTransferStatus.REJECTED.getCode();
        } else if (ProcessAction.TRANSFER_RECEIVE.getCode().equals(action)) {
            fromDeptName = transfer.getSourceDeptName();
            toDeptName = transfer.getTargetDeptName();
            status = CrossStreetTransferStatus.ACCEPTED.getCode();
        } else if (ProcessAction.TRANSFER_PROCESS.getCode().equals(action)) {
            status = CrossStreetTransferStatus.PROCESSING.getCode();
        } else if (ProcessAction.TRANSFER_COMPLETE.getCode().equals(action)) {
            status = CrossStreetTransferStatus.COMPLETED.getCode();
        }

        TransferTraceVO trace = buildTrace(sort, transferId, traceType, nodeName,
                process.getHandlerId(), process.getHandlerName(),
                fromDeptName, toDeptName,
                process.getHandleTime(),
                process.getComment(),
                status);

        if (process.getAttachments() != null && !process.getAttachments().isEmpty()) {
            trace.setAttachments(process.getAttachments());
        }
        trace.setAction(action);
        trace.setActionName(nodeName);
        if (process.getDurationSeconds() != null) {
            trace.setTraceDetail("耗时：" + formatDuration(process.getDurationSeconds()));
        }

        return trace;
    }

    private String formatDuration(Long seconds) {
        if (seconds == null || seconds <= 0) {
            return "0分钟";
        }
        if (seconds < 60) {
            return seconds + "秒";
        }
        if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours < 24) {
            return hours + "小时" + (minutes > 0 ? minutes + "分钟" : "");
        }
        long days = hours / 24;
        long remainHours = hours % 24;
        return days + "天" + (remainHours > 0 ? remainHours + "小时" : "");
    }

    @Override
    public List<DeptTreeVO> getCooperationDeptTree(String targetType, Long sourceDeptId) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1);
        wrapper.orderByAsc(SysDept::getSort);

        List<SysDept> allDepts = sysDeptMapper.selectList(wrapper);

        if (StrUtil.isNotBlank(targetType)) {
            if (TransferTargetType.STREET.getCode().equals(targetType)) {
                allDepts = allDepts.stream()
                        .filter(d -> d.getParentId() != null && d.getParentId() == 0L
                                || (d.getCode() != null && d.getCode().startsWith("STREET_")))
                        .collect(Collectors.toList());
            } else if (TransferTargetType.BUREAU.getCode().equals(targetType)) {
                allDepts = allDepts.stream()
                        .filter(d -> d.getCode() != null && d.getCode().startsWith("BUREAU_"))
                        .collect(Collectors.toList());
            } else if (TransferTargetType.COUNTY.getCode().equals(targetType)) {
                allDepts = allDepts.stream()
                        .filter(d -> d.getParentId() == null || d.getParentId() == 0L)
                        .collect(Collectors.toList());
            }
        }

        if (sourceDeptId != null) {
            allDepts = allDepts.stream()
                    .filter(d -> !d.getId().equals(sourceDeptId))
                    .collect(Collectors.toList());
        }

        Map<Long, DeptTreeVO> voMap = new HashMap<>();
        List<DeptTreeVO> rootList = new ArrayList<>();

        for (SysDept dept : allDepts) {
            DeptTreeVO vo = new DeptTreeVO();
            BeanUtils.copyProperties(dept, vo);
            vo.setDeptType(resolveDeptType(dept));
            vo.setDeptTypeName(TransferTargetType.getNameByCode(vo.getDeptType()));
            vo.setIsCrossStreetEnabled(true);
            voMap.put(dept.getId(), vo);
        }

        for (DeptTreeVO vo : voMap.values()) {
            if (vo.getParentId() == null || vo.getParentId() == 0L || !voMap.containsKey(vo.getParentId())) {
                rootList.add(vo);
            } else {
                DeptTreeVO parent = voMap.get(vo.getParentId());
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(vo);
            }
        }

        return rootList;
    }

    @Override
    public Map<String, Object> getTransferStatistics(Long deptId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<EventCrossStreetTransfer> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(EventCrossStreetTransfer::getSourceDeptId, deptId)
                .or().eq(EventCrossStreetTransfer::getTargetDeptId, deptId));

        List<EventCrossStreetTransfer> allTransfers = transferMapper.selectList(wrapper);

        long pendingApproval = allTransfers.stream()
                .filter(t -> CrossStreetTransferStatus.PENDING_APPROVAL.getCode().equals(t.getStatus()))
                .count();
        long transferred = allTransfers.stream()
                .filter(t -> CrossStreetTransferStatus.TRANSFERRED.getCode().equals(t.getStatus()))
                .count();
        long processing = allTransfers.stream()
                .filter(t -> CrossStreetTransferStatus.PROCESSING.getCode().equals(t.getStatus()))
                .count();
        long completed = allTransfers.stream()
                .filter(t -> CrossStreetTransferStatus.COMPLETED.getCode().equals(t.getStatus()))
                .count();
        long rejected = allTransfers.stream()
                .filter(t -> CrossStreetTransferStatus.REJECTED.getCode().equals(t.getStatus()))
                .count();

        long asSource = allTransfers.stream()
                .filter(t -> deptId.equals(t.getSourceDeptId()))
                .count();
        long asTarget = allTransfers.stream()
                .filter(t -> deptId.equals(t.getTargetDeptId()))
                .count();

        result.put("total", allTransfers.size());
        result.put("pendingApproval", pendingApproval);
        result.put("transferred", transferred);
        result.put("processing", processing);
        result.put("completed", completed);
        result.put("rejected", rejected);
        result.put("asSource", asSource);
        result.put("asTarget", asTarget);

        return result;
    }

    @Override
    public boolean isCrossStreetEvent(Long eventId, Long currentDeptId) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null || eventInfo.getGridId() == null) {
            return false;
        }

        GridInfo gridInfo = gridInfoMapper.selectById(eventInfo.getGridId());
        if (gridInfo == null) {
            return false;
        }

        List<GridInfo> allGrids = gridInfoMapper.selectList(null);
        Map<Long, Long> gridToDeptMap = new HashMap<>();
        for (GridInfo g : allGrids) {
            if (g.getGridLeaderId() != null) {
                SysUser leader = sysUserMapper.selectById(g.getGridLeaderId());
                if (leader != null && leader.getDeptId() != null) {
                    gridToDeptMap.put(g.getId(), leader.getDeptId());
                }
            }
        }

        Long eventDeptId = gridToDeptMap.get(eventInfo.getGridId());
        return eventDeptId != null && !eventDeptId.equals(currentDeptId);
    }

    @Override
    public List<Map<String, Object>> getRecommendedTargets(Long eventId, String targetType) {
        EventInfo eventInfo = eventInfoMapper.selectById(eventId);
        if (eventInfo == null) {
            throw new BusinessException("事件不存在");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        String eventType = eventInfo.getEventType();

        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getStatus, 1);

        if (StrUtil.isNotBlank(targetType)) {
            if (TransferTargetType.STREET.getCode().equals(targetType)) {
                wrapper.and(w -> w.isNull(SysDept::getParentId).or().eq(SysDept::getParentId, 0L));
            }
        }

        List<SysDept> depts = sysDeptMapper.selectList(wrapper);

        Map<String, Double> typeDeptScores = new HashMap<>();
        typeDeptScores.put("TRAFFIC_BUREAU", 1.0);
        typeDeptScores.put("ENVIRONMENT_BUREAU", 0.9);
        typeDeptScores.put("CONSTRUCTION_BUREAU", 0.85);
        typeDeptScores.put("CIVIL_AFFAIRS_BUREAU", 0.8);
        typeDeptScores.put("HEALTH_BUREAU", 0.75);
        typeDeptScores.put("POLICE_STATION", 0.9);
        typeDeptScores.put("FIRE_BRIGADE", 0.85);

        for (SysDept dept : depts) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", dept.getId());
            item.put("name", dept.getName());
            item.put("code", dept.getCode());
            item.put("leader", dept.getLeader());
            item.put("phone", dept.getPhone());

            double score = 0.5;
            if (dept.getCode() != null) {
                for (Map.Entry<String, Double> entry : typeDeptScores.entrySet()) {
                    if (dept.getCode().contains(entry.getKey())
                            || dept.getName().contains(entry.getKey().replace("_BUREAU", "").replace("_STATION", "").replace("_BRIGADE", ""))) {
                        score = entry.getValue();
                        break;
                    }
                }
            }

            if (eventType != null) {
                if (eventType.contains("交通") && dept.getName().contains("交通")) score = 1.0;
                if (eventType.contains("环境") && dept.getName().contains("环保")) score = 1.0;
                if (eventType.contains("污染") && dept.getName().contains("环保")) score = 1.0;
                if (eventType.contains("建设") && dept.getName().contains("住建")) score = 1.0;
                if (eventType.contains("治安") && dept.getName().contains("公安")) score = 1.0;
                if (eventType.contains("消防") && dept.getName().contains("消防")) score = 1.0;
                if (eventType.contains("医疗") && dept.getName().contains("卫生")) score = 1.0;
            }

            item.put("matchScore", Math.round(score * 100));
            item.put("matchReason", score >= 0.8 ? "职责高度匹配" : "可协调处理");
            item.put("targetType", resolveDeptType(dept));
            result.add(item);
        }

        result.sort((a, b) -> Double.compare(
                ((Number) b.get("matchScore")).doubleValue(),
                ((Number) a.get("matchScore")).doubleValue()
        ));

        return result;
    }

    private TransferTraceVO buildTrace(int sort, Long transferId, String traceType, String nodeName,
                                       Long operatorId, String operatorName,
                                       String fromDeptName, String toDeptName,
                                       LocalDateTime operateTime, String comment, String status) {
        TransferTraceVO trace = new TransferTraceVO();
        trace.setId((long) (sort + 1));
        trace.setTransferId(transferId);
        trace.setTraceType(traceType);
        trace.setTraceTypeName(nodeName);
        trace.setNodeName(nodeName);
        trace.setOperatorId(operatorId);
        trace.setOperatorName(operatorName);
        trace.setOperateTime(operateTime);
        trace.setComment(comment);
        trace.setFromDeptName(fromDeptName);
        trace.setToDeptName(toDeptName);
        trace.setStatus(status);
        trace.setStatusName(CrossStreetTransferStatus.getNameByCode(status));
        trace.setSortOrder(sort);
        return trace;
    }

    private CrossStreetTransferVO convertToSimpleVO(EventCrossStreetTransfer entity) {
        CrossStreetTransferVO vo = new CrossStreetTransferVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setStatusName(CrossStreetTransferStatus.getNameByCode(entity.getStatus()));
        vo.setTargetTypeName(TransferTargetType.getNameByCode(entity.getTargetType()));
        vo.setStatusTagType(getStatusTagType(entity.getStatus()));
        vo.setUrgencyLevelName(resolveUrgencyLevelName(entity.getUrgencyLevel()));

        if (entity.getAttachments() != null && !entity.getAttachments().isEmpty()) {
            vo.setAttachmentList(Arrays.asList(entity.getAttachments().split(",")));
        }

        return vo;
    }

    private CrossStreetTransferVO convertToVO(EventCrossStreetTransfer entity) {
        CrossStreetTransferVO vo = convertToSimpleVO(entity);
        vo.setTraceList(getTransferTrace(entity.getId()));

        Long currentUserId = DataScopeUtils.getCurrentUserId();
        if (currentUserId != null) {
            SysUser currentUser = sysUserMapper.selectById(currentUserId);
            if (currentUser != null) {
                vo.setCanApprove(CrossStreetTransferStatus.PENDING_APPROVAL.getCode().equals(entity.getStatus())
                        && canApproveTransfer(entity, currentUser));
                vo.setCanReceive(CrossStreetTransferStatus.TRANSFERRED.getCode().equals(entity.getStatus())
                        && currentUser.getDeptId() != null
                        && currentUser.getDeptId().equals(entity.getTargetDeptId()));
                vo.setCanProcess(CrossStreetTransferStatus.PROCESSING.getCode().equals(entity.getStatus())
                        && currentUser.getDeptId() != null
                        && currentUser.getDeptId().equals(entity.getTargetDeptId()));
                vo.setCanComplete(CrossStreetTransferStatus.PROCESSING.getCode().equals(entity.getStatus())
                        && currentUser.getDeptId() != null
                        && currentUser.getDeptId().equals(entity.getTargetDeptId()));
            }
        }

        return vo;
    }

    private String getStatusTagType(String status) {
        switch (CrossStreetTransferStatus.fromCode(status)) {
            case PENDING_APPROVAL: return "warning";
            case APPROVED:
            case TRANSFERRED: return "primary";
            case ACCEPTED:
            case PROCESSING: return "info";
            case COMPLETED: return "success";
            case REJECTED: return "danger";
            default: return "default";
        }
    }

    private String resolveUrgencyLevelName(String level) {
        if (level == null) return "普通";
        switch (level) {
            case "HIGH": return "紧急";
            case "MEDIUM": return "重要";
            case "LOW": return "普通";
            default: return level;
        }
    }

    private String resolveDeptType(SysDept dept) {
        if (dept.getCode() != null) {
            if (dept.getCode().startsWith("BUREAU_") || dept.getName().contains("局") || dept.getName().contains("委员会")) {
                return TransferTargetType.BUREAU.getCode();
            }
            if (dept.getCode().startsWith("STREET_") || dept.getName().contains("街道") || dept.getName().contains("镇")) {
                return TransferTargetType.STREET.getCode();
            }
            if ((dept.getParentId() == null || dept.getParentId() == 0L)
                    && (dept.getName().contains("区") || dept.getName().contains("市"))) {
                return TransferTargetType.COUNTY.getCode();
            }
        }
        if (dept.getParentId() == null || dept.getParentId() == 0L) {
            return TransferTargetType.STREET.getCode();
        }
        return TransferTargetType.BUREAU.getCode();
    }

    private boolean isApprover(SysUser user) {
        if (user.getRole() == null) return false;
        return user.getRole().contains("admin")
                || user.getRole().contains("street_manager")
                || user.getRole().contains("supervisor");
    }

    private boolean canApproveTransfer(EventCrossStreetTransfer transfer, SysUser user) {
        if (user == null || user.getRole() == null) {
            return false;
        }

        RoleEnum roleEnum = RoleEnum.getByCode(user.getRole());
        if (roleEnum == null) {
            return false;
        }

        if (RoleEnum.ADMIN.equals(roleEnum)) {
            return true;
        }

        if (RoleEnum.SUPERVISOR.equals(roleEnum)) {
            return true;
        }

        if (RoleEnum.STREET_MANAGER.equals(roleEnum)) {
            Long sourceDeptId = transfer.getSourceDeptId();
            Long userDeptId = user.getDeptId();

            if (sourceDeptId == null || userDeptId == null) {
                return false;
            }

            if (sourceDeptId.equals(userDeptId)) {
                return false;
            }

            return isParentDept(userDeptId, sourceDeptId);
        }

        return false;
    }

    private boolean isParentDept(Long parentDeptId, Long childDeptId) {
        if (parentDeptId == null || childDeptId == null) {
            return false;
        }

        SysDept childDept = sysDeptMapper.selectById(childDeptId);
        if (childDept == null) {
            return false;
        }

        if (parentDeptId.equals(childDept.getParentId())) {
            return true;
        }

        if (childDept.getParentId() != null && childDept.getParentId() != 0) {
            return isParentDept(parentDeptId, childDept.getParentId());
        }

        return false;
    }

    private Long getTopParentDeptId(Long deptId) {
        if (deptId == null) {
            return null;
        }

        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            return deptId;
        }

        if (dept.getParentId() == null || dept.getParentId() == 0) {
            return deptId;
        }

        return getTopParentDeptId(dept.getParentId());
    }

    private List<SysUser> findTransferApprovers(Long sourceDeptId) {
        List<SysUser> allApprovers = new ArrayList<>();

        LambdaQueryWrapper<SysUser> adminWrapper = new LambdaQueryWrapper<>();
        adminWrapper.eq(SysUser::getRole, "admin");
        adminWrapper.eq(SysUser::getStatus, 1);
        allApprovers.addAll(sysUserMapper.selectList(adminWrapper));

        LambdaQueryWrapper<SysUser> supervisorWrapper = new LambdaQueryWrapper<>();
        supervisorWrapper.eq(SysUser::getRole, "supervisor");
        supervisorWrapper.eq(SysUser::getStatus, 1);
        allApprovers.addAll(sysUserMapper.selectList(supervisorWrapper));

        if (sourceDeptId != null) {
            SysDept sourceDept = sysDeptMapper.selectById(sourceDeptId);
            if (sourceDept != null && sourceDept.getParentId() != null && sourceDept.getParentId() != 0) {
                LambdaQueryWrapper<SysDept> parentWrapper = new LambdaQueryWrapper<>();
                parentWrapper.eq(SysDept::getStatus, 1);
                List<SysDept> allDepts = sysDeptMapper.selectList(parentWrapper);

                List<Long> parentDeptIds = new ArrayList<>();
                collectParentDeptIds(sourceDeptId, allDepts, parentDeptIds);

                if (!parentDeptIds.isEmpty()) {
                    LambdaQueryWrapper<SysUser> streetMgrWrapper = new LambdaQueryWrapper<>();
                    streetMgrWrapper.eq(SysUser::getRole, "street_manager");
                    streetMgrWrapper.in(SysUser::getDeptId, parentDeptIds);
                    streetMgrWrapper.eq(SysUser::getStatus, 1);
                    allApprovers.addAll(sysUserMapper.selectList(streetMgrWrapper));
                }
            }
        }

        return allApprovers.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    private void collectParentDeptIds(Long deptId, List<SysDept> allDepts, List<Long> parentIds) {
        Map<Long, SysDept> deptMap = allDepts.stream()
                .collect(Collectors.toMap(SysDept::getId, d -> d));

        Long currentId = deptId;
        while (currentId != null) {
            SysDept dept = deptMap.get(currentId);
            if (dept == null || dept.getParentId() == null || dept.getParentId() == 0) {
                break;
            }
            parentIds.add(dept.getParentId());
            currentId = dept.getParentId();
        }
    }

    private void sendTransferNotification(EventCrossStreetTransfer transfer, String action, Long operatorId) {
        try {
            String title = "";
            String content = "";
            Long receiverId = null;

            switch (action) {
                case "PENDING_APPROVAL":
                    title = "【跨街道流转审批提醒】";
                    content = String.format("事件【%s】已提交跨街道流转申请，请及时审批。\n申请人：%s\n申请转至：%s",
                            transfer.getEventTitle(), transfer.getApplicantName(), transfer.getTargetDeptName());
                    List<SysUser> approvers = findTransferApprovers(transfer.getSourceDeptId());
                    for (SysUser approver : approvers) {
                        notificationService.sendAppPush(approver.getId(), title, content, "TRANSFER_APPROVAL", transfer.getId());
                    }
                    break;

                case "APPROVED":
                case "TRANSFERRED":
                    title = "【跨街道流转已转派】";
                    content = String.format("事件【%s】的跨街道流转申请已通过，已转至【%s】。\n审批人：%s",
                            transfer.getEventTitle(), transfer.getTargetDeptName(), transfer.getApproverName());
                    notificationService.sendAppPush(transfer.getApplicantId(), title, content, "TRANSFER_APPROVED", transfer.getId());

                    title = "【待接收：跨街道协作任务】";
                    content = String.format("收到来自【%s】的协作任务：【%s】\n请及时接收处理。",
                            transfer.getSourceDeptName(), transfer.getEventTitle());
                    List<SysUser> receivers = findDeptUsers(transfer.getTargetDeptId());
                    for (SysUser receiver : receivers) {
                        notificationService.sendAppPush(receiver.getId(), title, content, "TRANSFER_RECEIVE", transfer.getId());
                    }
                    break;

                case "REJECTED":
                    title = "【跨街道流转申请被驳回】";
                    content = String.format("事件【%s】的跨街道流转申请被驳回。\n审批人：%s\n驳回理由：%s",
                            transfer.getEventTitle(), transfer.getApproverName(),
                            transfer.getApproveComment() != null ? transfer.getApproveComment() : "暂无");
                    notificationService.sendAppPush(transfer.getApplicantId(), title, content, "TRANSFER_REJECTED", transfer.getId());
                    break;

                case "ACCEPTED":
                    title = "【协作任务已被接收】";
                    content = String.format("事件【%s】的协作任务已被【%s】接收并开始处理。",
                            transfer.getEventTitle(), transfer.getReceiverName());
                    notificationService.sendAppPush(transfer.getApplicantId(), title, content, "TRANSFER_ACCEPTED", transfer.getId());
                    notificationService.sendAppPush(transfer.getApproverId(), title, content, "TRANSFER_ACCEPTED", transfer.getId());
                    break;

                case "PROCESSING":
                    title = "【协作处理中更新】";
                    content = String.format("事件【%s】的协作处理有新进展。\n处理人：%s",
                            transfer.getEventTitle(), transfer.getHandlerName());
                    notificationService.sendAppPush(transfer.getApplicantId(), title, content, "TRANSFER_PROCESSING", transfer.getId());
                    break;

                case "COMPLETED":
                    title = "【协作任务已办结】";
                    content = String.format("事件【%s】的跨街道协作已办结。\n处理结果：%s\n处理人：%s",
                            transfer.getEventTitle(), transfer.getProcessResult(),
                            transfer.getHandlerName());
                    notificationService.sendAppPush(transfer.getApplicantId(), title, content, "TRANSFER_COMPLETED", transfer.getId());
                    if (transfer.getApproverId() != null) {
                        notificationService.sendAppPush(transfer.getApproverId(), title, content, "TRANSFER_COMPLETED", transfer.getId());
                    }
                    break;
            }

            log.info("跨街道流转通知发送成功，流转ID：{}，动作：{}", transfer.getId(), action);
        } catch (Exception e) {
            log.error("跨街道流转通知发送失败，流转ID：{}，错误：{}", transfer.getId(), e.getMessage(), e);
        }
    }

    private void validateTransferAuthority(EventInfo eventInfo, SysUser user) {
        if (eventInfo.getGridId() == null) {
            return;
        }

        RoleEnum roleEnum = RoleEnum.getByCode(user.getRole());
        if (roleEnum == null) {
            return;
        }

        if (RoleEnum.ADMIN.equals(roleEnum) || RoleEnum.HANDLER.equals(roleEnum)) {
            return;
        }

        boolean isEventInOwnScope = isGridInUserScope(eventInfo.getGridId(), user);

        if (isEventInOwnScope) {
            boolean hasCrossBoundaryFeature = checkCrossBoundaryFeature(eventInfo);
            if (!hasCrossBoundaryFeature) {
                throw new BusinessException(
                        "该事件属于本街道管辖范围，且未发现跨界特征。如确需跨街道协作，请在事件描述中注明跨界情况，或联系管理员确认。");
            }
        }
    }

    private boolean isGridInUserScope(Long gridId, SysUser user) {
        if (gridId == null || user.getGridId() == null) {
            return false;
        }

        if (gridId.equals(user.getGridId())) {
            return true;
        }

        List<Long> subGridIds = DataScopeUtils.getSubGridIds(user.getGridId());
        return subGridIds != null && subGridIds.contains(gridId);
    }

    private boolean checkCrossBoundaryFeature(EventInfo eventInfo) {
        String title = eventInfo.getTitle() != null ? eventInfo.getTitle() : "";
        String description = eventInfo.getDescription() != null ? eventInfo.getDescription() : "";
        String content = title + description;

        String[] crossBoundaryKeywords = {
                "跨街道", "跨界", "跨区", "相邻街道", "隔壁街道", "邻街",
                "上游", "下游", "过境", "交界", "边界", "接壤",
                "河道", "河流", "沟渠", "主干道", "快速路", "高速"
        };

        for (String keyword : crossBoundaryKeywords) {
            if (content.contains(keyword)) {
                return true;
            }
        }

        return false;
    }

    private void addEventProcessRecord(Long eventId, String action, String nodeName,
                                       Long handlerId, String handlerName,
                                       String comment, String attachments) {
        try {
            EventProcess process = new EventProcess();
            process.setEventId(eventId);
            process.setNodeName(nodeName);
            process.setHandlerId(handlerId);
            process.setHandlerName(handlerName);
            process.setAction(action);
            process.setComment(comment);
            if (StrUtil.isNotBlank(attachments)) {
                process.setAttachments(attachments);
            }
            process.setHandleTime(LocalDateTime.now());

            LambdaQueryWrapper<EventProcess> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(EventProcess::getEventId, eventId)
                    .orderByDesc(EventProcess::getHandleTime)
                    .last("LIMIT 1");
            EventProcess lastProcess = eventProcessMapper.selectOne(wrapper);
            if (lastProcess != null && lastProcess.getHandleTime() != null) {
                long seconds = java.time.temporal.ChronoUnit.SECONDS.between(
                        lastProcess.getHandleTime(), process.getHandleTime());
                process.setDurationSeconds(seconds);
            }

            eventProcessMapper.insert(process);
            log.debug("事件处置流水已记录，事件ID：{}，动作：{}，处理人：{}", eventId, action, handlerName);
        } catch (Exception e) {
            log.error("记录事件处置流水失败，事件ID：{}，动作：{}", eventId, action, e);
        }
    }

    private List<SysUser> findApprovers() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysUser::getRole, Arrays.asList("admin", "street_manager", "supervisor"));
        wrapper.eq(SysUser::getStatus, 1);
        return sysUserMapper.selectList(wrapper);
    }

    private List<SysUser> findDeptUsers(Long deptId) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getDeptId, deptId);
        wrapper.eq(SysUser::getStatus, 1);
        return sysUserMapper.selectList(wrapper);
    }
}
