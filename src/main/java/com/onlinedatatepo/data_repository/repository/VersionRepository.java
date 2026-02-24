package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.Version;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Version entity.
 * Provides database access methods for dataset version operations.
 */
@Repository
public interface VersionRepository extends JpaRepository<Version, Integer> {

    /**
     * Find all versions of a dataset ordered by version number descending.
     */
    List<Version> findByDataset_DatasetIdOrderByVersionNumberDesc(Integer datasetId);

    /**
     * Find a specific version by dataset and version number.
     */
    Optional<Version> findByDataset_DatasetIdAndVersionNumber(Integer datasetId, Integer versionNumber);

    /**

     * Count versions for a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
