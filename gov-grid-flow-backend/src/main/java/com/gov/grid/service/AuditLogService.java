package com.gov.grid.service;

import com.gov.grid.common.PageResult;
import com.gov.grid.dto.AuditLogQueryDTO;
import com.gov.grid.entity.AuditLog;
import com.gov.grid.vo.AuditLogVO;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface AuditLogService {

    void saveLog(AuditLog auditLog);

    PageResult<AuditLogVO> queryLogs(AuditLogQueryDTO queryDTO);

    AuditLogVO getLogById(String id);

    List<AuditLogVO> queryLogsForExport(AuditLogQueryDTO queryDTO);

    void exportPdf(AuditLogQueryDTO queryDTO, HttpServletResponse response) throws IOException;

    boolean verifyLogIntegrity(String id);

    void verifyAllLogsIntegrity();
}
