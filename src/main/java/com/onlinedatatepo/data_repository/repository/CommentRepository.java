package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Comment entity.
 * Provides database access methods for comment operations.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    /**
     * Find all top-level comments for a dataset.
     */
    @Query("SELECT c FROM Comment c WHERE c.dataset.datasetId = :datasetId AND c.parent IS NULL")
    List<Comment> findTopLevelCommentsByDatasetId(@Param("datasetId") Integer datasetId);

    /**
     * Find all comments for a dataset with pagination.
     */
    Page<Comment> findByDataset_DatasetId(Integer datasetId, Pageable pageable);

    /**
     * Find all comments by a user.
     */
    List<Comment> findByUser_UserId(Integer userId);

    /**
     * Find all replies to a comment with pagination.
     */
    Page<Comment> findByParent_CommentId(Integer parentId, Pageable pageable);

    /**
     * Count comments on a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);

    /**
     * Count replies to a comment.
     */
    long countByParent_CommentId(Integer parentId);
}
