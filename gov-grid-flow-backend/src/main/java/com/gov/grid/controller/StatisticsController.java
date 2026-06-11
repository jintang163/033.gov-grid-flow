package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.StatisticsService;
import com.gov.grid.vo.DeptStatsVO;
import com.gov.grid.vo.EventTrendVO;
import com.gov.grid.vo.EventTypeStatsVO;
import com.gov.grid.vo.GridStatsVO;
import com.gov.grid.vo.StatisticsVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "统计分析")
@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @ApiOperation("获取概览统计")
    @GetMapping("/overview")
    public Result<StatisticsVO> getOverviewStats() {
        StatisticsVO statisticsVO = statisticsService.getOverviewStats();
        return Result.success(statisticsVO);
    }

    @ApiOperation("获取事件趋势")
    @GetMapping("/trend")
    public Result<List<EventTrendVO>> getEventTrend(@RequestParam(required = false) Integer days) {
        List<EventTrendVO> list = statisticsService.getEventTrend(days);
        return Result.success(list);
    }

    @ApiOperation("获取事件类型统计")
    @GetMapping("/event-type")
    public Result<List<EventTypeStatsVO>> getEventTypeStats() {
        List<EventTypeStatsVO> list = statisticsService.getEventTypeStats();
        return Result.success(list);
    }

    @ApiOperation("获取部门统计")
    @GetMapping("/dept")
    public Result<List<DeptStatsVO>> getDeptStats() {
        List<DeptStatsVO> list = statisticsService.getDeptStats();
        return Result.success(list);
    }

    @ApiOperation("获取网格统计")
    @GetMapping("/grid")
    public Result<List<GridStatsVO>> getGridStats() {
        List<GridStatsVO> list = statisticsService.getGridStats();
        return Result.success(list);
    }
}
