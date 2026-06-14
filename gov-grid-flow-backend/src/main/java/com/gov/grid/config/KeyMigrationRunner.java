package com.gov.grid.config;

import com.gov.grid.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class KeyMigrationRunner implements ApplicationRunner {

    private final EncryptionService encryptionService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("[KeyMigration] 启动敏感密钥加密迁移检查...");
            encryptionService.migrateEncryptedKeys();
            log.info("[KeyMigration] 敏感密钥加密迁移检查完成");
        } catch (Exception e) {
            log.error("[KeyMigration] 敏感密钥加密迁移失败，请手动检查encryption_key表数据", e);
        }
    }
}
