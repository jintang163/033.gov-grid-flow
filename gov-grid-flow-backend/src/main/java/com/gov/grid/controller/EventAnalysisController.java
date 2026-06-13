package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.EventAnalysisService;
import com.gov.grid.vo.EventAnalysisReportVO;
import com.gov.grid.vo.EventGraphVO;
import com.gov.grid.vo.RecurrenceGroupVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "事件关联分析")
@RestController
@RequestMapping("/event/analysis")
@RequiredArgsConstructor
public class EventAnalysisController {

    private final EventAnalysisService eventAnalysisService;

    @ApiOperation("分析指定事件并标记高复发")
    @PostMapping("/analyze/{eventId}")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader')")
    public Result<Boolean> analyzeEvent(@PathVariable Long eventId) {
        boolean isHighRecurrence = eventAnalysisService.analyzeAndMarkRecurrence(eventId);
        return Result.success(isHighRecurrence);
    }

    @ApiOperation("批量扫描并标记高复发事件")
    @PostMapping("/scan")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    public Result<Integer> batchScanHighRecurrence() {
        int count = eventAnalysisService.batchScanAndMarkHighRecurrence();
        return Result.success("扫描完成，共标记" + count + "组高复发事件", count);
    }

    @ApiOperation("获取高复发事件分组列表")
    @GetMapping("/recurrence/list")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader','supervisor')")
    public Result<List<RecurrenceGroupVO>> listHighRecurrenceGroups(
            @RequestParam(required = false) Integer days) {
        List<RecurrenceGroupVO> list = eventAnalysisService.listHighRecurrenceGroups(days);
        return Result.success(list);
    }

    @ApiOperation("获取高复发分组详情")
    @GetMapping("/recurrence/{groupKey}")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader','supervisor')")
    public Result<RecurrenceGroupVO> getRecurrenceGroup(@PathVariable String groupKey) {
        RecurrenceGroupVO vo = eventAnalysisService.getRecurrenceGroup(groupKey);
        if (vo == null) {
            return Result.error("高复发分组不存在");
        }
        return Result.success(vo);
    }

    @ApiOperation("获取事件关联图谱")
    @GetMapping("/graph/{eventId}")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader','supervisor')")
    public Result<EventGraphVO> getEventRelationGraph(
            @PathVariable Long eventId,
            @RequestParam(required = false, defaultValue = "2") Integer depth) {
        EventGraphVO vo = eventAnalysisService.getEventRelationGraph(eventId, depth);
        if (vo == null) {
            return Result.error("事件不存在或无法生成关联图谱");
        }
        return Result.success(vo);
    }

    @ApiOperation("生成事件分析报告")
    @PostMapping("/report/generate")
    @PreAuthorize("hasAnyRole('admin','street_manager','supervisor')")
    public Result<EventAnalysisReportVO> generateReport(
            @RequestParam(required = false) Integer days) {
        EventAnalysisReportVO report = eventAnalysisService.generateAnalysisReport(days);
        return Result.success(report);
    }

    @ApiOperation("推送报告到街道办")
    @PostMapping("/report/push")
    @PreAuthorize("hasAnyRole('admin','street_manager')")
    public Result<Boolean> pushReport(@RequestParam(required = false) Integer days) {
        EventAnalysisReportVO report = eventAnalysisService.generateAnalysisReport(days);
        boolean pushed = eventAnalysisService.pushReportToStreetOffice(report);
        return Result.success(pushed ? "报告已推送到街道办" : "推送失败", pushed);
    }
}
