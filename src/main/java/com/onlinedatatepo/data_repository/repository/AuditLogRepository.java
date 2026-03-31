package com.onlinedatatepo.data_repository.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onlinedatatepo.data_repository.entity.AuditLog;

/**
 * Repository for AuditLog entity.
 * Provides database access methods for audit log operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

        interface DatasetActionCountProjection {
                Integer getDatasetId();
                String getAction();
                Long getTotal();
        }

        interface DatasetLeaderboardProjection {
                Integer getDatasetId();
                String getDatasetName();
                Long getTotal();
        }

        interface UserActivityProjection {
                Integer getUserId();
                String getDisplayName();
                Long getTotal();
        }

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

        /**
         * Count audit logs for a dataset filtered by action (case-insensitive).
         */
        long countByDataset_DatasetIdAndActionIgnoreCase(Integer datasetId, String action);

        @Query(value = """
                SELECT a.log_id, a.action, a.dataset_id, a.details, a.timestamp, a.user_id
                FROM audit_logs a
                WHERE (CAST(:userId AS integer) IS NULL OR a.user_id = CAST(:userId AS integer))
                  AND (CAST(:action AS varchar) IS NULL OR UPPER(CAST(a.action AS varchar)) = UPPER(CAST(:action AS varchar)))
                  AND (CAST(:startTime AS timestamp) IS NULL OR a.timestamp >= CAST(:startTime AS timestamp))
                  AND (CAST(:endTime AS timestamp) IS NULL OR a.timestamp <= CAST(:endTime AS timestamp))
                  AND (CAST(:datasetId AS integer) IS NULL OR a.dataset_id = CAST(:datasetId AS integer))
                ORDER BY a.timestamp DESC
                """, nativeQuery = true)
        Page<AuditLog> searchLogs(@Param("userId") Integer userId,
                                                            @Param("action") String action,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime,
                                                            @Param("datasetId") Integer datasetId,
                                                            Pageable pageable);

                                    @Query("""
                                            SELECT COUNT(a)
                                            FROM AuditLog a
                                            WHERE UPPER(CAST(a.action AS string)) = UPPER(CAST(:action AS string))
                                            """)
                                    long countByActionIgnoreCase(@Param("action") String action);

    @Query("""
            SELECT a.dataset.datasetId AS datasetId,
                   UPPER(a.action) AS action,
                   COUNT(a) AS total
            FROM AuditLog a
            WHERE a.dataset IS NOT NULL
              AND a.dataset.datasetId IN :datasetIds
              AND UPPER(a.action) IN :actions
            GROUP BY a.dataset.datasetId, UPPER(a.action)
            """)
    List<DatasetActionCountProjection> countByDatasetIdsAndActions(@Param("datasetIds") List<Integer> datasetIds,
                                                                    @Param("actions") List<String> actions);

    @Query("""
            SELECT a.dataset.datasetId AS datasetId,
                   COALESCE(a.dataset.name, 'Unknown') AS datasetName,
                   COUNT(a) AS total
            FROM AuditLog a
            WHERE a.dataset IS NOT NULL
              AND UPPER(a.action) = UPPER(:action)
            GROUP BY a.dataset.datasetId, a.dataset.name
            ORDER BY COUNT(a) DESC
            """)
    Page<DatasetLeaderboardProjection> topDatasetsByAction(@Param("action") String action, Pageable pageable);

    @Query("""
            SELECT a.user.userId AS userId,
                   COALESCE(a.user.fullName, a.user.username) AS displayName,
                   COUNT(a) AS total
            FROM AuditLog a
            WHERE a.user IS NOT NULL
            GROUP BY a.user.userId, a.user.fullName, a.user.username
            ORDER BY COUNT(a) DESC
            """)
    Page<UserActivityProjection> topActiveUsers(Pageable pageable);
}
