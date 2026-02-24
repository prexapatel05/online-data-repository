package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.DatasetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DatasetFile entity.
 * Provides database access methods for dataset file operations.
 */
@Repository
public interface DatasetFileRepository extends JpaRepository<DatasetFile, Integer> {

    /**
     * Find all files in a dataset.
     */
    List<DatasetFile> findByDataset_DatasetId(Integer datasetId);

    /**
     * Find a file by file name in a dataset.
     */
    DatasetFile findByFileNameAndDataset_DatasetId(String fileName, Integer datasetId);

    /**
     * Count files in a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
