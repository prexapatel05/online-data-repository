package com.onlinedatatepo.data_repository.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onlinedatatepo.data_repository.entity.DatasetFile;
import com.onlinedatatepo.data_repository.entity.DatasetFileCategory;

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
     * Find files in a dataset by category.
     */
    List<DatasetFile> findByDataset_DatasetIdAndFileCategory(Integer datasetId, DatasetFileCategory fileCategory);

    /**
     * Find files in a dataset by category ordered by upload time.
     */
    List<DatasetFile> findByDataset_DatasetIdAndFileCategoryOrderByUploadedAtAsc(Integer datasetId, DatasetFileCategory fileCategory);

    /**
     * Find a file by file name in a dataset.
     */
    DatasetFile findByFileNameAndDataset_DatasetId(String fileName, Integer datasetId);

    /**
     * Find a file by name and category in a dataset.
     */
    DatasetFile findByDataset_DatasetIdAndFileCategoryAndFileName(Integer datasetId,
                                                                   DatasetFileCategory fileCategory,
                                                                   String fileName);

    /**
     * Count files in a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
