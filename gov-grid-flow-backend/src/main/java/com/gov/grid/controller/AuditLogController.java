package com.gov.grid.controller;

import com.gov.grid.common.PageResult;
import com.gov.grid.common.Result;
import com.gov.grid.dto.AuditLogQueryDTO;
import com.gov.grid.service.AuditLogService;
import com.gov.grid.vo.AuditLogVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Api(tags = "审计日志管理")
@RestController
@RequestMapping("/audit-log")
public class AuditLogController {

    @Autowired
    private AuditLogService auditLogService;

    @ApiOperation("分页查询审计日志")
    @PostMapping("/query")
    public Result<PageResult<AuditLogVO>> queryLogs(@RequestBody AuditLogQueryDTO queryDTO) {
        return Result.success(auditLogService.queryLogs(queryDTO));
    }

    @ApiOperation("获取审计日志详情")
    @GetMapping("/{id}")
    public Result<AuditLogVO> getLogById(@PathVariable String id) {
        return Result.success(auditLogService.getLogById(id));
    }

    @ApiOperation("导出审计报告PDF")
    @PostMapping("/export-pdf")
    public void exportPdf(@RequestBody AuditLogQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        auditLogService.exportPdf(queryDTO, response);
    }

    @ApiOperation("验证单条日志自哈希完整性")
    @GetMapping("/verify/{id}")
    public Result<Boolean> verifyLogIntegrity(@PathVariable String id) {
        return Result.success(auditLogService.verifyLogIntegrity(id));
    }

    @ApiOperation("从指定日志向上校验完整哈希链")
    @GetMapping("/verify-chain/{id}")
    public Result<Map<String, Object>> verifyChainFromId(@PathVariable String id) {
        return Result.success(auditLogService.verifyChainFromId(id));
    }

    @ApiOperation("全局校验：从最新日志追溯到创世日志的完整哈希链")
    @PostMapping("/verify-all")
    public Result<Map<String, Object>> verifyAllLogsIntegrity() {
        return Result.success(auditLogService.verifyAllLogsIntegrity());
    }
}
