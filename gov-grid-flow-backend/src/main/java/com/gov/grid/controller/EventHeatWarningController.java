package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.service.EventHeatWarningService;
import com.gov.grid.vo.EventHeatForecastVO;
import com.gov.grid.vo.HeatmapCalendarVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "事件热度预警")
@RestController
@RequestMapping("/event-heat-warning")
@RequiredArgsConstructor
public class EventHeatWarningController {

    private final EventHeatWarningService eventHeatWarningService;

    @ApiOperation("获取所有网格热度预测")
    @GetMapping("/forecast")
    public Result<List<EventHeatForecastVO>> getGridHeatForecast(
            @RequestParam(required = false, defaultValue = "24") Integer hours) {
        List<EventHeatForecastVO> list = eventHeatWarningService.getGridHeatForecast(hours);
        return Result.success(list);
    }

    @ApiOperation("获取指定网格热度预测")
    @GetMapping("/forecast/{gridId}")
    public Result<EventHeatForecastVO> getGridHeatForecastByGridId(
            @PathVariable Long gridId,
            @RequestParam(required = false, defaultValue = "24") Integer hours) {
        EventHeatForecastVO vo = eventHeatWarningService.getGridHeatForecastByGridId(gridId, hours);
        return Result.success(vo);
    }

    @ApiOperation("获取日历热力图数据")
    @GetMapping("/calendar-heatmap")
    public Result<List<HeatmapCalendarVO>> getCalendarHeatmap(
            @RequestParam(required = false, defaultValue = "30") Integer days,
            @RequestParam(required = false) Long gridId) {
        List<HeatmapCalendarVO> list = eventHeatWarningService.getCalendarHeatmap(days, gridId);
        return Result.success(list);
    }

    @ApiOperation("获取指定月份日历热力图")
    @GetMapping("/calendar-heatmap/month")
    public Result<List<HeatmapCalendarVO>> getCalendarHeatmapByMonth(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestParam(required = false) Long gridId) {
        List<HeatmapCalendarVO> list = eventHeatWarningService.getCalendarHeatmapByMonth(year, month, gridId);
        return Result.success(list);
    }

    @ApiOperation("推送预警通知")
    @PostMapping("/push/{gridId}")
    public Result<Boolean> pushWarningNotification(@PathVariable Long gridId) {
        boolean result = eventHeatWarningService.pushWarningNotification(gridId);
        return Result.success(result);
    }

    @ApiOperation("获取高风险预警网格列表")
    @GetMapping("/high-warning")
    public Result<List<EventHeatForecastVO>> getHighWarningGrids() {
        List<EventHeatForecastVO> list = eventHeatWarningService.getHighWarningGrids();
        return Result.success(list);
    }
}
