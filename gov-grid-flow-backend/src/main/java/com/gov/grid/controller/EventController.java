package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.dto.BatchSyncRequestDTO;
import com.gov.grid.dto.BatchSyncResponseDTO;
import com.gov.grid.dto.EventProcessDTO;
import com.gov.grid.dto.EventQueryDTO;
import com.gov.grid.dto.EventReportDTO;
import com.gov.grid.entity.EventInfo;
import com.gov.grid.enums.EventStatus;
import com.gov.grid.enums.ProcessAction;
import com.gov.grid.common.PageResult;
import com.gov.grid.entity.EventUrgeRecord;
import com.gov.grid.service.EventProcessService;
import com.gov.grid.service.EventService;
import com.gov.grid.service.EventUrgeService;
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
    private final EventUrgeService eventUrgeService;

    @ApiOperation("上报事件（移动端/网格员）")
    @PostMapping("/report")
    public Result<EventInfo> reportEvent(@Validated @RequestBody EventReportDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean async = request.getHeader("X-Async") != null && "true".equalsIgnoreCase(request.getHeader("X-Async"));
        EventInfo eventInfo = eventService.reportEvent(dto, userId, async);
        return Result.success("事件上报成功", eventInfo);
    }

    @ApiOperation("批量同步上报（离线数据同步）")
    @PostMapping("/batch-sync")
    public Result<BatchSyncResponseDTO> batchSync(@Validated @RequestBody BatchSyncRequestDTO dto,
                                                  HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean async = request.getHeader("X-Async") != null && "true".equalsIgnoreCase(request.getHeader("X-Async"));

        BatchSyncResponseDTO response;
        if (async) {
            response = eventService.processBatchAsync(dto.getEvents(), userId, dto.getDeviceId());
        } else {
            response = eventService.processBatch(dto.getEvents(), userId);
        }

        String message = response.getMessage() != null ? response.getMessage() : "批量同步完成";
        return Result.success(message, response);
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

    @ApiOperation("根据clientId查询事件（幂等查询）")
    @GetMapping("/client/{clientId}")
    public Result<EventInfo> getEventByClientId(@PathVariable String clientId) {
        EventInfo eventInfo = eventService.findByClientId(clientId);
        if (eventInfo == null) {
            return Result.success("事件不存在", null);
        }
        return Result.success(eventInfo);
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

    @ApiOperation("我的催办列表（移动端）")
    @GetMapping("/reminders")
    public Result<PageResult<EventUrgeRecord>> getMyReminders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PageResult<EventUrgeRecord> page = eventUrgeService.getMyReminders(userId, pageNum, pageSize);
        return Result.success(page);
    }

    @ApiOperation("未读催办数量（移动端）")
    @GetMapping("/reminders/unread-count")
    public Result<Integer> getUnreadReminderCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Integer count = eventUrgeService.getUnreadReminderCount(userId);
        return Result.success(count);
    }

    @ApiOperation("标记催办已读（移动端）")
    @PostMapping("/reminders/{id}/read")
    public Result<Boolean> markReminderRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        boolean result = eventUrgeService.markReminderRead(id, userId);
        return Result.success(result ? "已标记" : "标记失败", result);
    }

    @ApiOperation("新增待办（自since时间起，移动端轮询）")
    @GetMapping("/my-todo/new")
    public Result<PageResult<EventInfo>> getNewTodoSince(
            @RequestParam(required = false) Long since,
            EventQueryDTO dto,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setStatus("PROCESSING");
        if (since != null && since > 0) {
            dto.setCreatedAtStart(new java.sql.Timestamp(since).toLocalDateTime());
        } else {
            dto.setCreatedAtStart(java.time.LocalDateTime.now().minusMinutes(5));
        }
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
