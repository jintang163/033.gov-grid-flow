package com.gov.grid.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.gov.grid.common.PageResult;
import com.gov.grid.dto.AuditLogQueryDTO;
import com.gov.grid.entity.AuditLog;
import com.gov.grid.repository.AuditLogRepository;
import com.gov.grid.service.AuditLogService;
import com.gov.grid.vo.AuditLogVO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    private static final String HASH_SALT = "gov-grid-flow-audit-log-salt-2024";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void saveLog(AuditLog auditLog) {
        String previousHash = getLastHash();
        auditLog.setPreviousHash(previousHash);

        String currentHash = calculateHash(auditLog);
        auditLog.setCurrentHash(currentHash);

        auditLogRepository.save(auditLog);
    }

    private String getLastHash() {
        try {
            Optional<AuditLog> lastLog = auditLogRepository.findTopByOrderByCreatedAtDesc();
            return lastLog.map(AuditLog::getCurrentHash).orElse("0");
        } catch (Exception e) {
            return "0";
        }
    }

    private String calculateHash(AuditLog log) {
        String content = log.getPreviousHash() + "|" +
                log.getUserId() + "|" +
                log.getUsername() + "|" +
                log.getEventId() + "|" +
                log.getOperation() + "|" +
                log.getModule() + "|" +
                log.getIpAddress() + "|" +
                log.getRequestUri() + "|" +
                log.getRequestParams() + "|" +
                log.getStatus() + "|" +
                (log.getCreatedAt() != null ? log.getCreatedAt().format(DATE_FORMATTER) : "") + "|" +
                HASH_SALT;
        return DigestUtil.sha256Hex(content);
    }

    @Override
    public PageResult<AuditLogVO> queryLogs(AuditLogQueryDTO queryDTO) {
        NativeSearchQuery query = buildQuery(queryDTO);
        SearchHits<AuditLog> searchHits = elasticsearchRestTemplate.search(query, AuditLog.class);

        List<AuditLogVO> voList = searchHits.getSearchHits().stream()
                .map(hit -> convertToVO(hit.getContent()))
                .collect(Collectors.toList());

        return PageResult.of(searchHits.getTotalHits(), voList, queryDTO.getPageNum(), queryDTO.getPageSize());
    }

    @Override
    public AuditLogVO getLogById(String id) {
        Optional<AuditLog> log = auditLogRepository.findById(id);
        return log.map(this::convertToVO).orElse(null);
    }

    @Override
    public List<AuditLogVO> queryLogsForExport(AuditLogQueryDTO queryDTO) {
        queryDTO.setPageNum(1);
        queryDTO.setPageSize(10000);
        NativeSearchQuery query = buildQuery(queryDTO);
        SearchHits<AuditLog> searchHits = elasticsearchRestTemplate.search(query, AuditLog.class);

        return searchHits.getSearchHits().stream()
                .map(hit -> convertToVO(hit.getContent()))
                .collect(Collectors.toList());
    }

    private NativeSearchQuery buildQuery(AuditLogQueryDTO queryDTO) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StrUtil.isNotBlank(queryDTO.getEventId())) {
            boolQuery.must(QueryBuilders.termQuery("eventId", queryDTO.getEventId()));
        }

        if (queryDTO.getUserId() != null) {
            boolQuery.must(QueryBuilders.termQuery("userId", queryDTO.getUserId()));
        }

        if (StrUtil.isNotBlank(queryDTO.getUsername())) {
            boolQuery.must(QueryBuilders.wildcardQuery("username", "*" + queryDTO.getUsername() + "*"));
        }

        if (StrUtil.isNotBlank(queryDTO.getModule())) {
            boolQuery.must(QueryBuilders.termQuery("module", queryDTO.getModule()));
        }

        if (StrUtil.isNotBlank(queryDTO.getOperation())) {
            boolQuery.must(QueryBuilders.termQuery("operation", queryDTO.getOperation()));
        }

        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            boolQuery.must(QueryBuilders.multiMatchQuery(queryDTO.getKeyword(),
                    "description", "requestParams", "responseResult", "errorMsg"));
        }

        if (queryDTO.getStatus() != null) {
            boolQuery.must(QueryBuilders.termQuery("status", queryDTO.getStatus()));
        }

        if (queryDTO.getStartTime() != null && queryDTO.getEndTime() != null) {
            boolQuery.must(QueryBuilders.rangeQuery("createdAt")
                    .gte(queryDTO.getStartTime())
                    .lte(queryDTO.getEndTime()));
        } else if (queryDTO.getStartTime() != null) {
            boolQuery.must(QueryBuilders.rangeQuery("createdAt").gte(queryDTO.getStartTime()));
        } else if (queryDTO.getEndTime() != null) {
            boolQuery.must(QueryBuilders.rangeQuery("createdAt").lte(queryDTO.getEndTime()));
        }

        int pageNum = Math.max(0, queryDTO.getPageNum() - 1);
        int pageSize = Math.min(10000, Math.max(1, queryDTO.getPageSize()));

        return new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withSort(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC))
                .withPageable(PageRequest.of(pageNum, pageSize))
                .build();
    }

    private AuditLogVO convertToVO(AuditLog log) {
        AuditLogVO vo = new AuditLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }

    @Override
    public void exportPdf(AuditLogQueryDTO queryDTO, HttpServletResponse response) throws IOException {
        List<AuditLogVO> logs = queryLogsForExport(queryDTO);

        response.setContentType("application/pdf");
        String fileName = "审计报告_" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + ".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=" +
                URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()));

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try (OutputStream out = response.getOutputStream()) {
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD, Color.BLACK);
            Font headerFont = new Font(bfChinese, 12, Font.BOLD, Color.WHITE);
            Font contentFont = new Font(bfChinese, 10, Font.NORMAL, Color.BLACK);

            Paragraph title = new Paragraph("审计日志报告", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Paragraph summary = new Paragraph("生成时间：" + LocalDateTime.now().format(DATE_FORMATTER)
                    + "    记录总数：" + logs.size() + "条", contentFont);
            summary.setSpacingAfter(15);
            document.add(summary);

            if (queryDTO.getStartTime() != null || queryDTO.getEndTime() != null) {
                String timeRange = "查询范围：";
                if (queryDTO.getStartTime() != null) {
                    timeRange += queryDTO.getStartTime().format(DATE_FORMATTER);
                }
                timeRange += " 至 ";
                if (queryDTO.getEndTime() != null) {
                    timeRange += queryDTO.getEndTime().format(DATE_FORMATTER);
                }
                Paragraph rangePara = new Paragraph(timeRange, contentFont);
                rangePara.setSpacingAfter(15);
                document.add(rangePara);
            }

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 2, 1.5f, 1.5f, 2, 3, 1, 2});

            String[] headers = {"操作时间", "操作人", "模块", "操作", "事件ID", "描述", "状态", "IP地址"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Paragraph(header, headerFont));
                cell.setBackgroundColor(new Color(51, 122, 183));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (AuditLogVO log : logs) {
                table.addCell(createCell(log.getCreatedAt() != null ? log.getCreatedAt().format(DATE_FORMATTER) : "", contentFont));
                table.addCell(createCell(log.getUsername(), contentFont));
                table.addCell(createCell(log.getModule(), contentFont));
                table.addCell(createCell(log.getOperation(), contentFont));
                table.addCell(createCell(log.getEventId(), contentFont));
                table.addCell(createCell(log.getDescription(), contentFont));
                table.addCell(createCell(log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败", contentFont));
                table.addCell(createCell(log.getIpAddress(), contentFont));
            }

            document.add(table);

            AtomicReference<String> integrityResult = new AtomicReference<>("完整性校验：通过");
            boolean allValid = logs.stream().allMatch(log -> {
                if (log.getId() != null) {
                    return verifyLogIntegrity(log.getId());
                }
                return true;
            });
            if (!allValid) {
                integrityResult.set("完整性校验：存在篡改记录");
            }

            Paragraph integrity = new Paragraph(integrityResult.get(), contentFont);
            integrity.setSpacingBefore(15);
            document.add(integrity);

            Paragraph footer = new Paragraph("本报告由系统自动生成，所有日志记录不可篡改。", contentFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            throw new IOException("PDF生成失败", e);
        }
    }

    private PdfPCell createCell(String content, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(content != null ? content : "-", font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        return cell;
    }

    @Override
    public boolean verifyLogIntegrity(String id) {
        Optional<AuditLog> logOpt = auditLogRepository.findById(id);
        if (!logOpt.isPresent()) {
            return false;
        }

        AuditLog log = logOpt.get();
        String calculatedHash = calculateHash(log);
        return calculatedHash.equals(log.getCurrentHash());
    }

    @Override
    public void verifyAllLogsIntegrity() {
    }
}
