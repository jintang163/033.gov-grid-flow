package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.dto.EvaluationDTO;
import com.gov.grid.entity.EventEvaluation;
import com.gov.grid.service.EvaluationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "满意度评价")
@RestController
@RequestMapping("/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @ApiOperation("提交评价")
    @PostMapping("/submit")
    public Result<EventEvaluation> submitEvaluation(@Validated @RequestBody EvaluationDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        EventEvaluation evaluation = evaluationService.submitEvaluation(dto, userId);
        return Result.success("评价提交成功", evaluation);
    }

    @ApiOperation("获取事件评价")
    @GetMapping("/event/{eventId}")
    public Result<EventEvaluation> getEvaluationByEventId(@PathVariable Long eventId) {
        EventEvaluation evaluation = evaluationService.getEvaluationByEventId(eventId);
        return Result.success(evaluation);
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
