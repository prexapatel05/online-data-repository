package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.DatasetAccess;
import com.onlinedatatepo.data_repository.entity.DatasetAccessId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for DatasetAccess entity.
 * Provides database access methods for managing authorized dataset access.
 */
@Repository
public interface DatasetAccessRepository extends JpaRepository<DatasetAccess, DatasetAccessId> {

    /**
     * Find all users authorized to access a dataset.
     */
    List<DatasetAccess> findByDataset_DatasetId(Integer datasetId);

    /**
     * Find all datasets a user is authorized to access.
     */
    List<DatasetAccess> findByUser_UserId(Integer userId);

    /**
     * Check if a user has access to a dataset.
     */
    boolean existsByUser_UserIdAndDataset_DatasetId(Integer userId, Integer datasetId);

    /**
     * Count users authorized to access a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
