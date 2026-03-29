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

    /**
     * Find all datasets by owner user ID with pagination.
     */
    Page<Dataset> findByUser_UserId(Integer userId, Pageable pageable);

    /**
     * Find datasets by access level with pagination.
     */
    Page<Dataset> findByAccessLevel(AccessLevel accessLevel, Pageable pageable);

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
}
