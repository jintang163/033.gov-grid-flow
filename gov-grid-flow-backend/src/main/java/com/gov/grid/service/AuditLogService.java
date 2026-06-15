package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.dto.AuditLogQueryDTO;
import com.gov.grid.entity.AuditLog;
import com.gov.grid.vo.AuditLogVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface AuditLogService {

    void saveLog(AuditLog auditLog);

    PageResult<AuditLogVO> queryLogs(AuditLogQueryDTO queryDTO);

    AuditLogVO getLogById(String id);

    List<AuditLogVO> queryLogsForExport(AuditLogQueryDTO queryDTO);

    void exportPdf(AuditLogQueryDTO queryDTO, HttpServletResponse response) throws IOException;

    boolean verifyLogIntegrity(String id);

    Map<String, Object> verifyChainFromId(String id);

    Map<String, Object> verifyAllLogsIntegrity();
}
