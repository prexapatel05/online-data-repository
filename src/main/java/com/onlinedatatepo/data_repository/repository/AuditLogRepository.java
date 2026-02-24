package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity.
 * Provides database access methods for audit log operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    /**
     * Find all audit logs for a user with pagination.
     */
    Page<AuditLog> findByUser_UserId(Integer userId, Pageable pageable);

    /**
     * Find all audit logs for a dataset with pagination.
     */
    Page<AuditLog> findByDataset_DatasetId(Integer datasetId, Pageable pageable);

    /**
     * Find audit logs by action with pagination.
     */
    Page<AuditLog> findByAction(String action, Pageable pageable);

    /**
     * Find audit logs within a time range.
     */
    List<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Count audit logs for a user.
     */
    long countByUser_UserId(Integer userId);

    /**
     * Count audit logs for a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
