package com.gov.grid.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.gov.grid.common.PageResult;
import com.gov.grid.dto.AuditLogQueryDTO;
import com.gov.grid.entity.AuditLog;
import com.gov.grid.mq.AuditLogMqProducer;
import com.gov.grid.repository.AuditLogRepository;
import com.gov.grid.service.AuditLogService;
import com.gov.grid.vo.AuditLogVO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private static final String HASH_SALT = "gov-grid-flow-audit-log-salt-2024";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String REDIS_LAST_HASH_KEY = "audit:log:last_hash";
    private static final String REDIS_HASH_LOCK_KEY = "audit:log:hash_lock";
    private static final String REDIS_LAST_ID_KEY = "audit:log:last_id";

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AuditLogMqProducer auditLogMqProducer;

    @Value("${audit.file-backup:true}")
    private boolean fileBackupEnabled;

    @Value("${audit.file-backup-path:./logs/audit}")
    private String fileBackupPath;

    @Value("${audit.mq-enabled:true}")
    private boolean mqEnabled;

    @Value("${audit.es-write-enabled:true}")
    private boolean esWriteEnabled;

    @Override
    public void saveLog(AuditLog auditLog) {
        if (auditLog.getId() == null) {
            auditLog.setId(IdUtil.simpleUUID());
        }

        String previousHash = acquireHashAndLock();
        auditLog.setPreviousHash(previousHash);

        String currentHash = calculateHash(auditLog);
        auditLog.setCurrentHash(currentHash);

        if (esWriteEnabled) {
            try {
                auditLogRepository.save(auditLog);
            } catch (Exception e) {
                log.error("审计日志写入ES失败, id={}, 降级仅文件备份+MQ", auditLog.getId(), e);
            }
        }

        if (mqEnabled) {
            auditLogMqProducer.sendAuditLog(auditLog);
        }

        if (fileBackupEnabled) {
            writeLocalBackup(auditLog);
        }

        updateLastHashAndUnlock(currentHash, auditLog.getId());
    }

    private String acquireHashAndLock() {
        try {
            String script =
                    "local lockKey = KEYS[1]\n" +
                    "local hashKey = KEYS[2]\n" +
                    "local lockVal = redis.call('SET', lockKey, '1', 'NX', 'EX', 5)\n" +
                    "if lockVal then\n" +
                    "  local hash = redis.call('GET', hashKey)\n" +
                    "  return hash or '0'\n" +
                    "else\n" +
                    "  return nil\n" +
                    "end";
            DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(String.class);

            for (int i = 0; i < 20; i++) {
                String result = redisTemplate.execute(redisScript,
                        Arrays.asList(REDIS_HASH_LOCK_KEY, REDIS_LAST_HASH_KEY));
                if (result != null) {
                    return result;
                }
                TimeUnit.MILLISECONDS.sleep(50);
            }
            log.warn("获取审计哈希锁超时，使用ES回退方式获取lastHash");
        } catch (Exception e) {
            log.warn("Redis哈希锁获取失败: {}", e.getMessage());
        }
        return getLastHashFromEsFallback();
    }

    private void updateLastHashAndUnlock(String currentHash, String logId) {
        try {
            String script =
                    "local lockKey = KEYS[1]\n" +
                    "local hashKey = KEYS[2]\n" +
                    "local idKey = KEYS[3]\n" +
                    "redis.call('SET', hashKey, ARGV[1])\n" +
                    "redis.call('SET', idKey, ARGV[2])\n" +
                    "redis.call('DEL', lockKey)\n" +
                    "return 1";
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            redisTemplate.execute(redisScript,
                    Arrays.asList(REDIS_HASH_LOCK_KEY, REDIS_LAST_HASH_KEY, REDIS_LAST_ID_KEY),
                    currentHash, logId);
        } catch (Exception e) {
            log.warn("更新Redis哈希和解锁失败: {}", e.getMessage());
            try {
                redisTemplate.delete(REDIS_HASH_LOCK_KEY);
            } catch (Exception ignored) {
            }
        }
    }

    private String getLastHashFromEsFallback() {
        try {
            String lastId = (String) redisTemplate.opsForValue().get(REDIS_LAST_ID_KEY);
            if (StrUtil.isNotBlank(lastId)) {
                Optional<AuditLog> lastLog = auditLogRepository.findById(lastId);
                if (lastLog.isPresent()) {
                    return lastLog.get().getCurrentHash();
                }
            }
        } catch (Exception ignored) {
        }
        try {
            SearchRequest searchRequest = new SearchRequest("audit_log-*");
            org.elasticsearch.action.search.SearchSourceBuilder sourceBuilder = new org.elasticsearch.action.search.SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.matchAllQuery());
            sourceBuilder.sort(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
            sourceBuilder.size(1);
            searchRequest.source(sourceBuilder);

            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            if (response.getHits().getTotalHits().value > 0) {
                Map<String, Object> sourceMap = response.getHits().getAt(0).getSourceAsMap();
                return (String) sourceMap.get("currentHash");
            }
        } catch (Exception e) {
            log.warn("ES回退查询lastHash失败: {}", e.getMessage());
        }
        return "0";
    }

    private void writeLocalBackup(AuditLog auditLog) {
        try {
            Path dir = Paths.get(fileBackupPath);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            String fileName = "audit-log-" + DateUtil.format(DateUtil.date(), "yyyyMMdd") + ".log";
            Path file = dir.resolve(fileName);
            String line = JSONUtil.toJsonStr(auditLog) + "\n";
            Files.write(file, line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            log.warn("审计日志本地备份写入失败: {}", e.getMessage());
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
        Optional<AuditLog> log = findLogCrossIndex(id);
        return log.map(this::convertToVO).orElse(null);
    }

    private Optional<AuditLog> findLogCrossIndex(String id) {
        try {
            return auditLogRepository.findById(id);
        } catch (Exception e) {
            log.debug("当前索引未找到id={}, 尝试跨索引搜索", id);
        }
        try {
            SearchRequest searchRequest = new SearchRequest("audit_log-*");
            org.elasticsearch.action.search.SearchSourceBuilder sourceBuilder = new org.elasticsearch.action.search.SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("_id", id));
            sourceBuilder.size(1);
            searchRequest.source(sourceBuilder);

            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            if (response.getHits().getTotalHits().value > 0) {
                SearchHit hit = response.getHits().getAt(0);
                String json = hit.getSourceAsString();
                AuditLog auditLog = JSONUtil.toBean(json, AuditLog.class);
                auditLog.setId(hit.getId());
                return Optional.of(auditLog);
            }
        } catch (Exception ex) {
            log.warn("跨索引搜索失败: {}", ex.getMessage());
        }
        return Optional.empty();
    }

    private AuditLog findLogByPreviousHash(String previousHash) {
        try {
            SearchRequest searchRequest = new SearchRequest("audit_log-*");
            org.elasticsearch.action.search.SearchSourceBuilder sourceBuilder = new org.elasticsearch.action.search.SearchSourceBuilder();
            sourceBuilder.query(QueryBuilders.termQuery("currentHash", previousHash));
            sourceBuilder.size(1);
            searchRequest.source(sourceBuilder);

            SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            if (response.getHits().getTotalHits().value > 0) {
                SearchHit hit = response.getHits().getAt(0);
                String json = hit.getSourceAsString();
                AuditLog auditLog = JSONUtil.toBean(json, AuditLog.class);
                auditLog.setId(hit.getId());
                return auditLog;
            }
        } catch (Exception ex) {
            log.warn("按previousHash跨索引搜索失败: {}", ex.getMessage());
        }
        return null;
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
                .withPageable(org.springframework.data.domain.PageRequest.of(pageNum, pageSize))
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

            Map<String, Object> chainResult;
            if (!logs.isEmpty() && logs.get(0).getId() != null) {
                chainResult = verifyChainFromId(logs.get(0).getId());
            } else {
                chainResult = new HashMap<>();
                chainResult.put("valid", true);
            }

            boolean valid = (Boolean) chainResult.getOrDefault("valid", true);
            String integrityText = valid ? "完整性校验：通过" :
                    "完整性校验：发现异常（位置" + chainResult.getOrDefault("brokenAt", "-") + "）";
            Paragraph integrity = new Paragraph(integrityText, contentFont);
            integrity.setSpacingBefore(15);
            document.add(integrity);

            Paragraph footer = new Paragraph("本报告由系统自动生成，所有日志采用哈希链式存储，不可篡改。", contentFont);
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
        Optional<AuditLog> logOpt = findLogCrossIndex(id);
        if (!logOpt.isPresent()) {
            return false;
        }
        AuditLog log = logOpt.get();
        String calculatedHash = calculateHash(log);
        return calculatedHash.equals(log.getCurrentHash());
    }

    @Override
    public Map<String, Object> verifyChainFromId(String id) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> chainPath = new ArrayList<>();
        boolean valid = true;
        String brokenAt = null;
        int verifiedCount = 0;
        int maxDepth = 10000;

        String currentId = id;
        while (currentId != null && verifiedCount < maxDepth) {
            Optional<AuditLog> logOpt = findLogCrossIndex(currentId);
            if (!logOpt.isPresent()) {
                valid = false;
                brokenAt = currentId;
                break;
            }

            AuditLog log = logOpt.get();
            Map<String, Object> step = new HashMap<>();
            step.put("id", log.getId());
            step.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().format(DATE_FORMATTER) : null);
            step.put("username", log.getUsername());
            step.put("selfHashValid", calculateHash(log).equals(log.getCurrentHash()));

            if (!calculateHash(log).equals(log.getCurrentHash())) {
                valid = false;
                brokenAt = "SELF:" + log.getId();
                chainPath.add(step);
                break;
            }

            String prevHash = log.getPreviousHash();
            if ("0".equals(prevHash)) {
                chainPath.add(step);
                verifiedCount++;
                break;
            }

            AuditLog prevLog = findLogByPreviousHash(prevHash);
            if (prevLog == null) {
                valid = false;
                brokenAt = "MISSING_PREV:" + prevHash;
                chainPath.add(step);
                break;
            }

            if (!prevHash.equals(prevLog.getCurrentHash())) {
                valid = false;
                brokenAt = "CHAIN:" + log.getId();
                chainPath.add(step);
                break;
            }

            chainPath.add(step);
            currentId = prevLog.getId();
            verifiedCount++;
        }

        result.put("valid", valid);
        result.put("verifiedCount", verifiedCount);
        result.put("brokenAt", brokenAt);
        result.put("chainPath", chainPath);
        return result;
    }

    @Override
    public Map<String, Object> verifyAllLogsIntegrity() {
        Map<String, Object> result = new HashMap<>();
        long total = 0;
        try {
            SearchRequest countReq = new SearchRequest("audit_log-*");
            org.elasticsearch.action.search.SearchSourceBuilder cb = new org.elasticsearch.action.search.SearchSourceBuilder();
            cb.query(QueryBuilders.matchAllQuery());
            cb.size(0);
            countReq.source(cb);
            SearchResponse countResp = restHighLevelClient.search(countReq, RequestOptions.DEFAULT);
            total = countResp.getHits().getTotalHits().value;
        } catch (Exception e) {
            result.put("error", "统计总数失败: " + e.getMessage());
            total = -1;
        }

        String newestId = null;
        try {
            SearchRequest sr = new SearchRequest("audit_log-*");
            org.elasticsearch.action.search.SearchSourceBuilder sb = new org.elasticsearch.action.search.SearchSourceBuilder();
            sb.query(QueryBuilders.matchAllQuery());
            sb.sort(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC));
            sb.size(1);
            sr.source(sb);
            SearchResponse sresp = restHighLevelClient.search(sr, RequestOptions.DEFAULT);
            if (sresp.getHits().getTotalHits().value > 0) {
                newestId = sresp.getHits().getAt(0).getId();
            }
        } catch (Exception e) {
            result.put("error", "查找最新日志失败: " + e.getMessage());
        }

        result.put("total", total);
        if (newestId != null) {
            result.put("newestId", newestId);
            Map<String, Object> chainResult = verifyChainFromId(newestId);
            result.putAll(chainResult);
        } else {
            result.put("valid", true);
            result.put("verifiedCount", 0);
            result.put("note", "无日志可校验");
        }
        return result;
    }
}
