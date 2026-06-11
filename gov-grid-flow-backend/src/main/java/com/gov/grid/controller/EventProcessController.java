package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.dto.EventProcessDTO;
import com.gov.grid.entity.EventProcess;
import com.gov.grid.service.EventProcessService;
import com.gov.grid.workflow.WorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.flowable.task.api.Task;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "事件流转")
@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class EventProcessController {

    private final EventProcessService eventProcessService;
    private final WorkflowService workflowService;

    @ApiOperation("处理任务（审核/处置/核查）")
    @PostMapping("/handle")
    public Result<Void> handleTask(@Validated @RequestBody EventProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        eventProcessService.processEvent(dto, userId);
        return Result.success("任务处理成功", null);
    }

    @ApiOperation("分派任务")
    @PostMapping("/assign")
    public Result<Void> assignTask(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long eventId = Long.valueOf(params.get("eventId").toString());
        String taskId = params.get("taskId") != null ? params.get("taskId").toString() : null;
        String assigneeId = params.get("assigneeId").toString();
        eventProcessService.assignTask(eventId, taskId, assigneeId, userId);
        return Result.success("任务分派成功", null);
    }

    @ApiOperation("退回任务")
    @PostMapping("/reject")
    public Result<Void> rejectTask(@Validated @RequestBody EventProcessDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        dto.setAction(ProcessAction.REJECT.getCode());
        eventProcessService.processEvent(dto, userId);
        return Result.success("任务退回成功", null);
    }

    @ApiOperation("获取处理历史")
    @GetMapping("/history/{eventId}")
    public Result<List<EventProcess>> getProcessHistory(@PathVariable Long eventId) {
        List<EventProcess> history = eventProcessService.getProcessHistory(eventId);
        return Result.success(history);
    }

    @ApiOperation("获取流程图")
    @GetMapping("/diagram/{eventId}")
    public Result<String> getProcessDiagram(@PathVariable Long eventId) {
        String diagram = eventProcessService.getProcessDiagram(eventId);
        return Result.success(diagram);
    }

    @ApiOperation("我的待办任务列表")
    @GetMapping("/todo")
    public Result<List<Map<String, Object>>> getTodoList(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        List<Task> tasks = workflowService.getTaskList(String.valueOf(userId), null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Task task : tasks) {
            Map<String, Object> item = new HashMap<>();
            item.put("taskId", task.getId());
            item.put("taskName", task.getName());
            item.put("processInstanceId", task.getProcessInstanceId());
            item.put("createTime", task.getCreateTime());
            item.put("assignee", task.getAssignee());
            result.add(item);
        }
        return Result.success(result);
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
