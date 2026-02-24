package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.Bookmark;
import com.onlinedatatepo.data_repository.entity.BookmarkId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Bookmark entity.
 * Provides database access methods for bookmark operations.
 */
@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, BookmarkId> {

    /**
     * Find all bookmarks for a user with pagination.
     */
    Page<Bookmark> findByUser_UserId(Integer userId, Pageable pageable);

    /**
     * Find all bookmarks for a dataset.
     */
    List<Bookmark> findByDataset_DatasetId(Integer datasetId);

    /**
     * Check if a user has bookmarked a dataset.
     */
    boolean existsByUser_UserIdAndDataset_DatasetId(Integer userId, Integer datasetId);

    /**
     * Count bookmarks for a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
