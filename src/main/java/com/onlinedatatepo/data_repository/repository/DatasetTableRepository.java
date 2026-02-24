package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.DatasetTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DatasetTable entity.
 * Provides database access methods for dataset table/schema operations.
 */
@Repository
public interface DatasetTableRepository extends JpaRepository<DatasetTable, Integer> {

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
}
