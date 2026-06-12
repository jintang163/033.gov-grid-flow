package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.dto.EventProcessDTO;
import com.gov.grid.dto.EventQueryDTO;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.ProcessAction;
import com.gov.grid.service.EventProcessService;
import com.gov.grid.service.EventService;
import com.gov.grid.vo.EventDetailVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "事件管理")
@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventProcessService eventProcessService;

    @ApiOperation("上报事件（移动端/网格员）")
    @PostMapping("/report")
    public Result<EventInfo> reportEvent(@Validated @RequestBody EventReportDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        EventInfo eventInfo = eventService.reportEvent(dto, userId);
        return Result.success("事件上报成功", eventInfo);
    }

    @ApiOperation("匿名上报事件（居民）")
    @PostMapping("/report/anonymous")
    public Result<EventInfo> reportEventAnonymous(@Validated @RequestBody EventReportDTO dto) {
        dto.setAnonymous(1);
        EventInfo eventInfo = eventService.reportEvent(dto, null);
        return Result.success("匿名事件上报成功", eventInfo);
    }

    @ApiOperation("事件列表")
    @GetMapping("/list")
    public Result<PageResult<EventInfo>> getEventList(EventQueryDTO dto) {
        PageResult<EventInfo> pageResult = eventService.getEventList(dto);
        return Result.success(pageResult);
    }

    @ApiOperation("事件详情")
    @GetMapping("/{id}")
    public Result<EventDetailVO> getEventDetail(@PathVariable Long id) {
        EventDetailVO detail = eventService.getEventDetail(id);
        return Result.success(detail);
    }

    @ApiOperation("我的待办")
    @GetMapping("/my-todo")
    public Result<PageResult<EventInfo>> getMyTodo(EventQueryDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setStatus("PROCESSING");
        PageResult<EventInfo> pageResult = eventService.getEventList(dto);
        return Result.success(pageResult);
    }

    @ApiOperation("我的已办")
    @GetMapping("/my-done")
    public Result<PageResult<EventInfo>> getMyDone(EventQueryDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setStatus(EventStatus.COMPLETED.getCode());
        PageResult<EventInfo> pageResult = eventService.getEventList(dto);
        return Result.success(pageResult);
    }

    @ApiOperation("获取事件类型列表")
    @GetMapping("/type/list")
    public Result<List<Map<String, String>>> getEventTypeList() {
        List<Map<String, String>> types = new ArrayList<>();
        Map<String, String> type1 = new HashMap<>();
        type1.put("code", "environment");
        type1.put("name", "环境卫生");
        types.add(type1);
        Map<String, String> type2 = new HashMap<>();
        type2.put("code", "public_facility");
        type2.put("name", "公共设施");
        types.add(type2);
        Map<String, String> type3 = new HashMap<>();
        type3.put("code", "dispute");
        type3.put("name", "矛盾纠纷");
        types.add(type3);
        Map<String, String> type4 = new HashMap<>();
        type4.put("code", "safety_hazard");
        type4.put("name", "安全隐患");
        types.add(type4);
        Map<String, String> type5 = new HashMap<>();
        type5.put("code", "traffic");
        type5.put("name", "交通出行");
        types.add(type5);
        Map<String, String> type6 = new HashMap<>();
        type6.put("code", "service");
        type6.put("name", "民生服务");
        types.add(type6);
        Map<String, String> type7 = new HashMap<>();
        type7.put("code", "security");
        type7.put("name", "治安问题");
        types.add(type7);
        Map<String, String> type8 = new HashMap<>();
        type8.put("code", "other");
        type8.put("name", "其他问题");
        types.add(type8);
        return Result.success(types);
    }

    @ApiOperation("获取我上报的事件列表")
    @GetMapping("/my-report")
    public Result<PageResult<EventInfo>> getMyReport(EventQueryDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setReporterId(userId);
        PageResult<EventInfo> pageResult = eventService.getEventList(dto);
        return Result.success(pageResult);
    }

    @ApiOperation("审核通过")
    @PostMapping("/approve")
    @PreAuthorize("hasRole('admin') or hasRole('street_manager') or hasRole('grid_leader')")
    public Result<Void> approve(@RequestBody EventProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setAction(ProcessAction.APPROVE.getCode());
        eventProcessService.processEvent(dto, userId);
        return Result.success("审核通过成功", null);
    }

    @ApiOperation("核查通过")
    @PostMapping("/verify")
    @PreAuthorize("hasRole('admin') or hasRole('street_manager') or hasRole('supervisor')")
    public Result<Void> verify(@RequestBody EventProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setAction(ProcessAction.VERIFY.getCode());
        eventProcessService.processEvent(dto, userId);
        return Result.success("核查通过成功", null);
    }

    @ApiOperation("处置完成")
    @PostMapping("/process")
    @PreAuthorize("hasRole('admin') or hasRole('handler')")
    public Result<Void> process(@RequestBody EventProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setAction(ProcessAction.HANDLE.getCode());
        eventProcessService.processEvent(dto, userId);
        return Result.success("处置完成成功", null);
    }

    @ApiOperation("退回")
    @PostMapping("/return")
    @PreAuthorize("hasRole('admin') or hasRole('street_manager') or hasRole('grid_leader')")
    public Result<Void> returnTask(@RequestBody EventProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setAction(ProcessAction.REJECT.getCode());
        eventProcessService.processEvent(dto, userId);
        return Result.success("退回成功", null);
    }

    @ApiOperation("分派")
    @PostMapping("/assign")
    @PreAuthorize("hasRole('admin') or hasRole('street_manager') or hasRole('grid_leader')")
    public Result<Void> assignTask(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long eventId = Long.valueOf(params.get("eventId").toString());
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : null;
        String assigneeId = params.get("assigneeId").toString();
        eventProcessService.assignTask(eventId, taskId, assigneeId, userId);
        return Result.success("分派成功", null);
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
}
