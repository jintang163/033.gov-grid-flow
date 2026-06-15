package com.gov.grid.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Document(indexName = "audit_log-#{T(java.time.LocalDate).now().format(T(java.time.format.DateTimeFormatter).ofPattern('yyyyMM'))}")
public class AuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String eventId;

    @Field(type = FieldType.Keyword)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Keyword)
    private String userRole;

    @Field(type = FieldType.Keyword)
    private String operation;

    @Field(type = FieldType.Keyword)
    private String module;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Keyword)
    private String method;

    @Field(type = FieldType.Keyword)
    private String requestUri;

    @Field(type = FieldType.Keyword)
    private String ipAddress;

    @Field(type = FieldType.Keyword)
    private String userAgent;

    @Field(type = FieldType.Object)
    private String requestParams;

    @Field(type = FieldType.Object)
    private String responseResult;

    @Field(type = FieldType.Keyword)
    private Integer status;

    @Field(type = FieldType.Text)
    private String errorMsg;

    @Field(type = FieldType.Long)
    private Long costTime;

    @Field(type = FieldType.Keyword)
    private String previousHash;

    @Field(type = FieldType.Keyword)
    private String currentHash;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
