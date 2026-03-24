package com.onlinedatatepo.data_repository.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Applies lightweight schema fixes for existing databases.
 */
@Component
public class SchemaPatchConfig {

    private final JdbcTemplate jdbcTemplate;

    public SchemaPatchConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureDatasetFileCategoryColumn() {
        jdbcTemplate.execute("ALTER TABLE dataset_files ADD COLUMN IF NOT EXISTS file_category VARCHAR(50)");
        jdbcTemplate.update("UPDATE dataset_files SET file_category = 'DATASET' WHERE file_category IS NULL");
    }
}