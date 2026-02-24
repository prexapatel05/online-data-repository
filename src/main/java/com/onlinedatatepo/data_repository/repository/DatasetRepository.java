package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
