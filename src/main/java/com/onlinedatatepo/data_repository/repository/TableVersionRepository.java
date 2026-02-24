package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.TableVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TableVersion entity.
 * Provides database access methods for table versioning operations.
 */
@Repository
public interface TableVersionRepository extends JpaRepository<TableVersion, Integer> {

    /**
     * Find all version records for a table.
     */
    List<TableVersion> findByTable_TableId(Integer tableId);

    /**
     * Find all tables in a version.
     */
    List<TableVersion> findByVersion_VersionId(Integer versionId);

    /**
     * Check if a table exists in a version.
     */
    boolean existsByTable_TableIdAndVersion_VersionId(Integer tableId, Integer versionId);

    /**
     * Count table versions for a table.
     */
    long countByTable_TableId(Integer tableId);

    /**
     * Count table versions in a dataset version.
     */
    long countByVersion_VersionId(Integer versionId);
}
