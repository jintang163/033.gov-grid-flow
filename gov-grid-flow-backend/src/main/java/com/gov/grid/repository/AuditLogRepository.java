package com.gov.grid.repository;

import com.gov.grid.entity.AuditLog;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends ElasticsearchRepository<AuditLog, String> {

    Optional<AuditLog> findTopByOrderByCreatedAtDesc();

    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
}
