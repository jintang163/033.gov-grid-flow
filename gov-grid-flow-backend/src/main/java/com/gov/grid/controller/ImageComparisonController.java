package com.gov.grid.controller;

import com.gov.grid.common.Result;
import com.gov.grid.dto.ImageComparisonDTO;
import com.gov.grid.dto.ImageComparisonResultVO;
import com.gov.grid.security.SecurityUtils;
import com.gov.grid.service.ImageComparisonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cv")
@RequiredArgsConstructor
public class ImageComparisonController {

    private final ImageComparisonService imageComparisonService;

    @PostMapping("/compare")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader','handler','supervisor','worker')")
    public Result<ImageComparisonResultVO> compare(@Validated @RequestBody ImageComparisonDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("图像比对请求，事件ID：{}，操作人：{}", dto.getEventId(), userId);
        ImageComparisonResultVO result = imageComparisonService.compareAndSave(dto, userId);
        return Result.success("比对完成", result);
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('admin','street_manager','grid_leader','handler','supervisor','worker')")
    public Result<List<ImageComparisonResultVO>> getComparisonHistory(@PathVariable Long eventId) {
        log.info("查询事件比对历史，事件ID：{}", eventId);
        List<ImageComparisonResultVO> list = imageComparisonService.getComparisonHistory(eventId);
        return Result.success(list);
    }
}
