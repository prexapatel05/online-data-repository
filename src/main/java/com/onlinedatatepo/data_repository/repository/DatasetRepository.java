package com.onlinedatatepo.data_repository.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetStatus;
import com.onlinedatatepo.data_repository.entity.FileType;

/**
 * Repository for Dataset entity.
 * Provides database access methods for dataset operations.
 */
@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Integer> {

  interface DatasetGrowthProjection {
    String getDay();
    Long getTotal();
  }

    /**
     * Find all datasets by owner user ID with pagination.
     */
    Page<Dataset> findByUser_UserId(Integer userId, Pageable pageable);

    /**
     * Find datasets by access level with pagination.
     */
    Page<Dataset> findByAccessLevel(AccessLevel accessLevel, Pageable pageable);

        @Query(value = """
            SELECT d.*
            FROM dataset d
            WHERE d.user_id = :userId
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            ORDER BY d.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM dataset d
            WHERE d.user_id = :userId
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            """,
            nativeQuery = true)
        Page<Dataset> searchOwnedDatasets(@Param("userId") Integer userId,
                                       @Param("search") String search,
                                       @Param("category") String category,
                                       @Param("visibility") String visibility,
                                       @Param("fileType") String fileType,
                                       Pageable pageable);

    /**
     * Count datasets by owner.
     */
    long countByUser_UserId(Integer userId);

    /**
     * Count datasets by status.
     */
    long countByStatus(DatasetStatus status);

    /**
     * Find verified datasets by access level.
     */
    @Query("SELECT d FROM Dataset d WHERE d.status = 'VERIFIED' AND d.accessLevel = :accessLevel")
    List<Dataset> findVerifiedDatasetsByAccessLevel(@Param("accessLevel") AccessLevel accessLevel);

        @Query("""
              SELECT d
              FROM Dataset d
              WHERE (
                  d.accessLevel = com.onlinedatatepo.data_repository.entity.AccessLevel.PUBLIC
                OR d.user.userId = :currentUserId
                OR (
                  d.accessLevel = com.onlinedatatepo.data_repository.entity.AccessLevel.AUTHORIZED
                  AND EXISTS (
                    SELECT 1 FROM d.authorizedUsers au
                    WHERE au.userId = :currentUserId
                  )
                )
              )
                AND (:search IS NULL
                    OR CAST(COALESCE(d.name, '') AS string) LIKE CONCAT('%', CAST(:search AS string), '%')
                  OR CAST(COALESCE(d.description, '') AS string) LIKE CONCAT('%', CAST(:search AS string), '%')
                  OR CAST(COALESCE(d.tag, '') AS string) LIKE CONCAT('%', CAST(:search AS string), '%'))
                AND (:category IS NULL OR CAST(COALESCE(d.tag, '') AS string) LIKE CONCAT('%', CAST(:category AS string), '%'))
                AND (:ownerId IS NULL OR d.user.userId = :ownerId)
                AND (:visibility IS NULL OR d.accessLevel = :visibility)
                AND (:fileType IS NULL OR EXISTS (
                    SELECT 1 FROM DatasetTable t
                    WHERE t.dataset = d AND t.fileType = :fileType
                ))
              """)
        Page<Dataset> searchAccessibleDatasets(@Param("currentUserId") Integer currentUserId,
                                       @Param("search") String search,
                                       @Param("category") String category,
                                       @Param("ownerId") Integer ownerId,
                                       @Param("visibility") AccessLevel visibility,
                                       @Param("fileType") FileType fileType,
                                       Pageable pageable);

        @Query(value = """
            SELECT d.*
            FROM dataset d
            LEFT JOIN (
              SELECT a.dataset_id, COUNT(*) AS view_count
              FROM audit_logs a
              WHERE UPPER(a.action) = 'DATASET_VIEWED'
              GROUP BY a.dataset_id
            ) v ON v.dataset_id = d.dataset_id
            WHERE (
              d.access_level = 'PUBLIC'
              OR d.user_id = :currentUserId
              OR (
                d.access_level = 'AUTHORIZED'
                AND EXISTS (
                  SELECT 1
                  FROM dataset_access da
                  WHERE da.dataset_id = d.dataset_id
                  AND da.user_id = :currentUserId
                )
              )
            )
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:ownerId IS NULL OR d.user_id = :ownerId)
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            ORDER BY COALESCE(v.view_count, 0) DESC, d.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM dataset d
            WHERE (
              d.access_level = 'PUBLIC'
              OR d.user_id = :currentUserId
              OR (
                d.access_level = 'AUTHORIZED'
                AND EXISTS (
                  SELECT 1
                  FROM dataset_access da
                  WHERE da.dataset_id = d.dataset_id
                  AND da.user_id = :currentUserId
                )
              )
            )
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:ownerId IS NULL OR d.user_id = :ownerId)
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            """,
            nativeQuery = true)
        Page<Dataset> searchAccessibleDatasetsOrderByViews(@Param("currentUserId") Integer currentUserId,
                                   @Param("search") String search,
                                   @Param("category") String category,
                                   @Param("ownerId") Integer ownerId,
                                   @Param("visibility") String visibility,
                                   @Param("fileType") String fileType,
                                   Pageable pageable);

        @Query(value = """
            SELECT d.*
            FROM dataset d
            LEFT JOIN (
              SELECT dataset_id, AVG(rating_value) AS avg_rating
              FROM rates
              GROUP BY dataset_id
            ) r ON r.dataset_id = d.dataset_id
            WHERE (
              d.access_level = 'PUBLIC'
              OR d.user_id = :currentUserId
              OR (
                d.access_level = 'AUTHORIZED'
                AND EXISTS (
                  SELECT 1
                  FROM dataset_access da
                  WHERE da.dataset_id = d.dataset_id
                  AND da.user_id = :currentUserId
                )
              )
            )
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:ownerId IS NULL OR d.user_id = :ownerId)
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            ORDER BY COALESCE(r.avg_rating, -1) DESC, d.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM dataset d
            WHERE (
              d.access_level = 'PUBLIC'
              OR d.user_id = :currentUserId
              OR (
                d.access_level = 'AUTHORIZED'
                AND EXISTS (
                  SELECT 1
                  FROM dataset_access da
                  WHERE da.dataset_id = d.dataset_id
                  AND da.user_id = :currentUserId
                )
              )
            )
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:ownerId IS NULL OR d.user_id = :ownerId)
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            """,
            nativeQuery = true)
        Page<Dataset> searchAccessibleDatasetsOrderByRating(@Param("currentUserId") Integer currentUserId,
                                   @Param("search") String search,
                                   @Param("category") String category,
                                   @Param("ownerId") Integer ownerId,
                                   @Param("visibility") String visibility,
                                   @Param("fileType") String fileType,
                                   Pageable pageable);

        @Query("""
              SELECT d
              FROM Dataset d
              JOIN d.authorizedUsers au
              WHERE d.accessLevel = com.onlinedatatepo.data_repository.entity.AccessLevel.AUTHORIZED
                AND au.userId = :currentUserId
                AND d.user.userId <> :currentUserId
              ORDER BY d.createdAt DESC
              """)
        Page<Dataset> findSharedWithMe(@Param("currentUserId") Integer currentUserId, Pageable pageable);

        @Query(value = """
            SELECT d.*
            FROM dataset d
            WHERE d.access_level = 'AUTHORIZED'
              AND d.user_id <> :currentUserId
              AND EXISTS (
                SELECT 1
                FROM dataset_access da
                WHERE da.dataset_id = d.dataset_id
                  AND da.user_id = :currentUserId
              )
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            ORDER BY d.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM dataset d
            WHERE d.access_level = 'AUTHORIZED'
              AND d.user_id <> :currentUserId
              AND EXISTS (
                SELECT 1
                FROM dataset_access da
                WHERE da.dataset_id = d.dataset_id
                  AND da.user_id = :currentUserId
              )
            AND (:search IS NULL
              OR COALESCE(d.name, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.description, '') ILIKE CONCAT('%', :search, '%')
              OR COALESCE(d.tag, '') ILIKE CONCAT('%', :search, '%'))
            AND (:category IS NULL OR COALESCE(d.tag, '') ILIKE CONCAT('%', :category, '%'))
            AND (:visibility IS NULL OR d.access_level = :visibility)
            AND (:fileType IS NULL OR EXISTS (
              SELECT 1 FROM dataset_tables t
              WHERE t.dataset_id = d.dataset_id AND t.file_type = :fileType
            ))
            """,
            nativeQuery = true)
        Page<Dataset> searchSharedWithMeDatasets(@Param("currentUserId") Integer currentUserId,
                                              @Param("search") String search,
                                              @Param("category") String category,
                                              @Param("visibility") String visibility,
                                              @Param("fileType") String fileType,
                                              Pageable pageable);

        @Query("""
              SELECT d
              FROM Dataset d
              WHERE (d.accessLevel = com.onlinedatatepo.data_repository.entity.AccessLevel.PUBLIC OR d.user.userId = :currentUserId)
                AND (:search IS NULL
                    OR CAST(COALESCE(d.name, '') AS string) LIKE CONCAT('%', CAST(:search AS string), '%')
                  OR CAST(COALESCE(d.description, '') AS string) LIKE CONCAT('%', CAST(:search AS string), '%')
                  OR CAST(COALESCE(d.tag, '') AS string) LIKE CONCAT('%', CAST(:search AS string), '%'))
                AND (:category IS NULL OR CAST(COALESCE(d.tag, '') AS string) LIKE CONCAT('%', CAST(:category AS string), '%'))
              ORDER BY d.createdAt DESC
              """)
        Page<Dataset> searchTrendingForDashboard(@Param("currentUserId") Integer currentUserId,
                                        @Param("search") String search,
                                        @Param("category") String category,
                                        Pageable pageable);

      @Query(value = """
          SELECT TO_CHAR(DATE(d.created_at), 'YYYY-MM-DD') AS day,
               COUNT(*) AS total
          FROM dataset d
          GROUP BY DATE(d.created_at)
          ORDER BY DATE(d.created_at) ASC
          """, nativeQuery = true)
      List<DatasetGrowthProjection> datasetGrowthTimeline();

        @Query(value = """
          SELECT TO_CHAR(DATE_TRUNC('week', d.created_at), 'YYYY-MM-DD') AS day,
             COUNT(*) AS total
          FROM dataset d
          GROUP BY DATE_TRUNC('week', d.created_at)
          ORDER BY DATE_TRUNC('week', d.created_at) ASC
          """, nativeQuery = true)
        List<DatasetGrowthProjection> datasetGrowthTimelineWeekly();

        @Query(value = """
          SELECT TO_CHAR(DATE_TRUNC('month', d.created_at), 'YYYY-MM') AS day,
             COUNT(*) AS total
          FROM dataset d
          GROUP BY DATE_TRUNC('month', d.created_at)
          ORDER BY DATE_TRUNC('month', d.created_at) ASC
          """, nativeQuery = true)
        List<DatasetGrowthProjection> datasetGrowthTimelineMonthly();
}
