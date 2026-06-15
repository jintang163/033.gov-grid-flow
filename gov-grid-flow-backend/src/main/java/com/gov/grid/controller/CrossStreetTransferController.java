package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.dto.CrossStreetTransferApproveDTO;
import com.gov.grid.dto.CrossStreetTransferDTO;
import com.gov.grid.dto.CrossStreetTransferProcessDTO;
import com.gov.grid.entity.SysUser;
import com.gov.grid.service.CrossStreetTransferService;
import com.gov.grid.vo.CrossStreetTransferVO;
import com.gov.grid.vo.DeptTreeVO;
import com.gov.grid.vo.TransferTraceVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@Api(tags = "跨街道协同流转")
@RestController
@RequestMapping("/cross-street-transfer")
@RequiredArgsConstructor
public class CrossStreetTransferController {

    private final CrossStreetTransferService crossStreetTransferService;

    @ApiOperation("提交跨街道流转申请")
    @PostMapping("/apply")
    public Result<Long> applyTransfer(@Validated @RequestBody CrossStreetTransferDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long transferId = crossStreetTransferService.applyTransfer(dto, userId);
        return Result.success("申请提交成功", transferId);
    }

    @ApiOperation("审批流转申请")
    @PostMapping("/approve")
    public Result<Void> approveTransfer(@Validated @RequestBody CrossStreetTransferApproveDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        crossStreetTransferService.approveTransfer(dto, userId);
        return Result.success(dto.getApproved() ? "审批通过" : "已驳回", null);
    }

    @ApiOperation("接收流转任务")
    @PostMapping("/receive/{transferId}")
    public Result<Void> receiveTransfer(@PathVariable Long transferId, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        crossStreetTransferService.receiveTransfer(transferId, userId);
        return Result.success("接收成功，已开始处理", null);
    }

    @ApiOperation("处理流转（添加处理记录）")
    @PostMapping("/process")
    public Result<Void> processTransfer(@Validated @RequestBody CrossStreetTransferProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        crossStreetTransferService.processTransfer(dto, userId);
        return Result.success("处理记录已添加", null);
    }

    @ApiOperation("办结流转")
    @PostMapping("/complete")
    public Result<Void> completeTransfer(@Validated @RequestBody CrossStreetTransferProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        crossStreetTransferService.completeTransfer(dto, userId);
        return Result.success("协作任务已办结", null);
    }

    @ApiOperation("获取流转详情")
    @GetMapping("/{transferId}")
    public Result<CrossStreetTransferVO> getTransferDetail(@PathVariable Long transferId) {
        CrossStreetTransferVO detail = crossStreetTransferService.getTransferDetail(transferId);
        return Result.success(detail);
    }

    @ApiOperation("分页查询流转列表")
    @GetMapping("/page")
    public Result<PageResult<CrossStreetTransferVO>> getTransferPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long sourceDeptId,
            @RequestParam(required = false) Long targetDeptId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PageResult<CrossStreetTransferVO> page = crossStreetTransferService.getTransferPage(
                pageNum, pageSize, eventId, sourceDeptId, targetDeptId, status, targetType, userId, keyword
        );
        return Result.success(page);
    }

    @ApiOperation("我参与的流转列表")
    @GetMapping("/my-involved")
    public Result<List<CrossStreetTransferVO>> getMyInvolvedTransfers(
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long deptId = getCurrentUserDeptId(request);
        List<CrossStreetTransferVO> list = crossStreetTransferService.getMyInvolvedTransfers(userId, deptId, status);
        return Result.success(list);
    }

    @ApiOperation("获取事件的流转历史")
    @GetMapping("/event/{eventId}")
    public Result<List<CrossStreetTransferVO>> getEventTransferHistory(@PathVariable Long eventId) {
        List<CrossStreetTransferVO> history = crossStreetTransferService.getEventTransferHistory(eventId);
        return Result.success(history);
    }

    @ApiOperation("获取流转追溯链")
    @GetMapping("/trace/{transferId}")
    public Result<List<TransferTraceVO>> getTransferTrace(@PathVariable Long transferId) {
        List<TransferTraceVO> trace = crossStreetTransferService.getTransferTrace(transferId);
        return Result.success(trace);
    }

    @ApiOperation("获取协作机构树（选择目标机构用）")
    @GetMapping("/cooperation-dept-tree")
    public Result<List<DeptTreeVO>> getCooperationDeptTree(
            @RequestParam(required = false) String targetType,
            HttpServletRequest request) {
        Long sourceDeptId = getCurrentUserDeptId(request);
        List<DeptTreeVO> tree = crossStreetTransferService.getCooperationDeptTree(targetType, sourceDeptId);
        return Result.success(tree);
    }

    @ApiOperation("获取流转统计")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getTransferStatistics(HttpServletRequest request) {
        Long deptId = getCurrentUserDeptId(request);
        Map<String, Object> stats = crossStreetTransferService.getTransferStatistics(deptId);
        return Result.success(stats);
    }

    @ApiOperation("检查事件是否需要跨街道处理")
    @GetMapping("/check-cross-street/{eventId}")
    public Result<Boolean> checkCrossStreet(@PathVariable Long eventId, HttpServletRequest request) {
        Long deptId = getCurrentUserDeptId(request);
        boolean isCross = crossStreetTransferService.isCrossStreetEvent(eventId, deptId);
        return Result.success(isCross);
    }

    @ApiOperation("获取推荐的协作机构")
    @GetMapping("/recommend-targets/{eventId}")
    public Result<List<Map<String, Object>>> getRecommendedTargets(
            @PathVariable Long eventId,
            @RequestParam(required = false) String targetType) {
        List<Map<String, Object>> targets = crossStreetTransferService.getRecommendedTargets(eventId, targetType);
        return Result.success(targets);
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr != null && !userIdStr.isEmpty()) {
            try {
                return Long.parseLong(userIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Long getCurrentUserDeptId(HttpServletRequest request) {
        String deptIdStr = request.getHeader("X-Dept-Id");
        if (deptIdStr != null && !deptIdStr.isEmpty()) {
            try {
                return Long.parseLong(deptIdStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
