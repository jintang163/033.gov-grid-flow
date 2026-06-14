package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.DashboardService;
import com.gov.grid.service.EventProcessService;
import com.gov.grid.vo.CommunityRankVO;
import com.gov.grid.vo.DashboardOverviewVO;
import com.gov.grid.vo.EventMarkerVO;
import com.gov.grid.vo.GridMemberStatusVO;
import com.gov.grid.vo.HeatmapPointVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "指挥调度大屏")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final EventProcessService eventProcessService;

    @ApiOperation("获取大屏概览数据")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> getOverview() {
        return Result.success(dashboardService.getDashboardOverview());
    }

    @ApiOperation("获取待处置事件标记")
    @GetMapping("/event-markers")
    public Result<List<EventMarkerVO>> getEventMarkers() {
        return Result.success(dashboardService.getPendingEventMarkers());
    }

    @ApiOperation("获取事件热力图数据")
    @GetMapping("/heatmap")
    public Result<List<HeatmapPointVO>> getHeatmap() {
        return Result.success(dashboardService.getEventHeatmap());
    }

    @ApiOperation("获取社区结案率排名")
    @GetMapping("/community-rank")
    public Result<List<CommunityRankVO>> getCommunityRank() {
        return Result.success(dashboardService.getCommunityRank());
    }

    @ApiOperation("获取网格员在线状态")
    @GetMapping("/member-status")
    public Result<List<GridMemberStatusVO>> getMemberStatus() {
        return Result.success(dashboardService.getGridMemberStatus());
    }

    @ApiOperation("获取事件详情")
    @GetMapping("/event/{eventId}")
    public Result<EventMarkerVO> getEventDetail(@PathVariable Long eventId) {
        return Result.success(dashboardService.getEventDetail(eventId));
    }

    @ApiOperation("一键派单")
    @PostMapping("/dispatch")
    public Result<Void> dispatchEvent(@RequestBody Map<String, Object> params) {
        Long eventId = Long.valueOf(params.get("eventId").toString());
        String assigneeId = params.get("assigneeId").toString();
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : null;
        Long operatorId = params.get("operatorId") != null ? Long.valueOf(params.get("operatorId").toString()) : 1L;
        eventProcessService.assignTask(eventId, taskId, assigneeId, operatorId);
        return Result.success("派单成功", null);
    }

    @ApiOperation("获取大屏全部数据")
    @GetMapping("/all")
    public Result<Map<String, Object>> getAllData() {
        return Result.success(dashboardService.getDashboardAllData());
    }
}
