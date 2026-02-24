package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.DatasetColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DatasetColumn entity.
 * Provides database access methods for dataset column metadata operations.
 */
@Repository
public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, Integer> {

    /**
     * Find all columns in a table.
     */
    List<DatasetColumn> findByTable_TableId(Integer tableId);

    /**
     * Find a column by name in a table.
     */
    DatasetColumn findByColumnNameAndTable_TableId(String columnName, Integer tableId);

    /**
     * Count columns in a table.
     */
    long countByTable_TableId(Integer tableId);
}
