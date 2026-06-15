package com.gov.grid.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.gov.grid.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${spring.elasticsearch.uris}")
    private String esUris;

    @Override
    public org.elasticsearch.client.RestHighLevelClient elasticsearchClient() {
        return org.springframework.data.elasticsearch.client.ClientConfiguration.create(esUris).rest();
    }
}
