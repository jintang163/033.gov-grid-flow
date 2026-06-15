package com.gov.grid.config;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONUtil;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ElasticsearchInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchInitializer.class);

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void run(ApplicationArguments args) {
        try {
            initIlmPolicy();
            initIndexTemplate();
            log.info("Elasticsearch初始化完成");
        } catch (Exception e) {
            log.warn("Elasticsearch初始化失败，请手动配置ILM策略和索引模板: {}", e.getMessage());
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
}
