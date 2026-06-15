package com.gov.grid.config;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class ElasticsearchInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchInitializer.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Value("${audit.readonly-previous-months:true}")
    private boolean readonlyPreviousMonths;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initIlmPolicy();
            initIndexTemplate();
            ensureCurrentIndexAndAlias();
            applyReadOnlyToPreviousMonths();
            log.info("Elasticsearch初始化完成");
        } catch (Exception e) {
            log.warn("Elasticsearch初始化失败，请手动配置ILM策略和索引模板: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 5 0 1 * ?")
    public void monthlyMaintenance() {
        log.info("执行月度审计索引维护");
        try {
            ensureCurrentIndexAndAlias();
            applyReadOnlyToPreviousMonths();
        } catch (Exception e) {
            log.warn("月度索引维护失败: {}", e.getMessage());
        }
    }

    private void initIlmPolicy() throws IOException {
        ClassPathResource resource = new ClassPathResource("es/audit-log-ilm-policy.json");
        String policyJson = IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);

        Request request = new Request("PUT", "/_ilm/policy/audit-log-policy");
        request.setJsonEntity(policyJson);

        try {
            restHighLevelClient.getLowLevelClient().performRequest(request);
            log.info("审计日志ILM策略创建成功");
        } catch (Exception e) {
            log.warn("审计日志ILM策略创建失败: {}", e.getMessage());
        }
    }

    private void initIndexTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("es/audit-log-index-template.json");
        String templateJson = IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);

        PutIndexTemplateRequest request = new PutIndexTemplateRequest("audit-log-template");
        request.source(templateJson, XContentType.JSON);

        try {
            restHighLevelClient.indices().putTemplate(request, RequestOptions.DEFAULT);
            log.info("审计日志索引模板创建成功");
        } catch (Exception e) {
            log.warn("审计日志索引模板创建失败: {}", e.getMessage());
        }
    }

    private void ensureCurrentIndexAndAlias() {
        try {
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String indexName = "audit_log-" + currentMonth;

            Request existsReq = new Request("HEAD", "/" + indexName);
            try {
                restHighLevelClient.getLowLevelClient().performRequest(existsReq);
                log.debug("当月索引{}已存在", indexName);
            } catch (Exception e) {
                Request createReq = new Request("PUT", "/" + indexName);
                Map<String, Object> body = new HashMap<>();
                Map<String, Object> aliases = new HashMap<>();
                aliases.put("audit_log_all", new HashMap<>());
                aliases.put("audit_log", new HashMap<String, Object>() {{
                    put("is_write_index", true);
                }});
                body.put("aliases", aliases);
                createReq.setJsonEntity(JSONUtil.toJsonStr(body));
                try {
                    restHighLevelClient.getLowLevelClient().performRequest(createReq);
                    log.info("当月审计索引{}创建成功", indexName);
                } catch (Exception ex) {
                    log.warn("当月审计索引创建失败: {}", ex.getMessage());
                }
            }

            Request switchAlias = new Request("POST", "/_aliases");
            String switchJson = "{\n" +
                    "  \"actions\": [\n" +
                    "    { \"remove\": { \"index\": \"audit_log-*\", \"alias\": \"audit_log\" } },\n" +
                    "    { \"add\": { \"index\": \"" + indexName + "\", \"alias\": \"audit_log\", \"is_write_index\": true } }\n" +
                    "  ]\n" +
                    "}";
            switchAlias.setJsonEntity(switchJson);
            try {
                restHighLevelClient.getLowLevelClient().performRequest(switchAlias);
                log.info("写入别名切换到{}成功", indexName);
            } catch (Exception ex) {
                log.warn("写入别名切换失败: {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.warn("初始化当月索引失败: {}", e.getMessage());
        }
    }

    private void applyReadOnlyToPreviousMonths() {
        if (!readonlyPreviousMonths) {
            return;
        }
        try {
            Request catReq = new Request("GET", "/_cat/indices/audit_log-*?format=json");
            String resp = IoUtil.read(
                    restHighLevelClient.getLowLevelClient().performRequest(catReq).getEntity().getContent(),
                    StandardCharsets.UTF_8);
            JSONArray indices = JSONUtil.parseArray(resp);
            String currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String currentIndex = "audit_log-" + currentMonth;

            for (int i = 0; i < indices.size(); i++) {
                JSONObject idx = indices.getJSONObject(i);
                String idxName = idx.getStr("index");
                if (idxName == null || idxName.equals(currentIndex)) continue;

                try {
                    UpdateSettingsRequest settingsReq = new UpdateSettingsRequest(idxName);
                    Settings settings = Settings.builder()
                            .put("index.blocks.write", true)
                            .put("index.blocks.metadata", false)
                            .build();
                    settingsReq.settings(settings);
                    restHighLevelClient.indices().putSettings(settingsReq, RequestOptions.DEFAULT);
                    log.info("索引{}已设置为只读（禁止修改/删除，仅允许查询）", idxName);
                } catch (Exception ex) {
                    log.warn("设置索引{}只读失败: {}", idxName, ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("设置历史索引只读失败: {}", e.getMessage());
        }
    }
}
