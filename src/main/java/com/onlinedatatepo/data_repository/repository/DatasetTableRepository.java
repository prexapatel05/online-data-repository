package com.onlinedatatepo.data_repository.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onlinedatatepo.data_repository.entity.DatasetTable;

/**
 * Repository for DatasetTable entity.
 * Provides database access methods for dataset table/schema operations.
 */
@Repository
public interface DatasetTableRepository extends JpaRepository<DatasetTable, Integer> {

    interface DatasetFileTypeProjection {
        Integer getDatasetId();
        String getFileType();
    }

    /**
     * Find all tables in a dataset.
     */
    List<DatasetTable> findByDataset_DatasetId(Integer datasetId);

    /**
     * Find a table by name within a dataset.
     */
    DatasetTable findByTableNameAndDataset_DatasetId(String tableName, Integer datasetId);

    /**
     * Count tables in a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);

    @Query("""
            SELECT t.dataset.datasetId AS datasetId, CAST(t.fileType AS string) AS fileType
            FROM DatasetTable t
            WHERE t.dataset.datasetId IN :datasetIds
            ORDER BY t.dataset.datasetId ASC, t.tableId ASC
            """)
    List<DatasetFileTypeProjection> findFileTypesByDatasetIds(@Param("datasetIds") List<Integer> datasetIds);
}
