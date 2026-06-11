package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.entity.SysNotification;
import com.gov.grid.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Api(tags = "消息通知")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @ApiOperation("获取未读消息数量")
    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    @ApiOperation("获取通知列表")
    @GetMapping("/list")
    public Result<PageResult<SysNotification>> getNotificationList(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        PageResult<SysNotification> pageResult = notificationService.getNotificationList(userId, pageNum, pageSize);
        return Result.success(pageResult);
    }

    @ApiOperation("标记单条通知已读")
    @PutMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    @ApiOperation("全部标记已读")
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        notificationService.markAllAsRead(userId);
        return Result.success();
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
