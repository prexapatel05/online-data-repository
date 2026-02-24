package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Rating entity.
 * Provides database access methods for rating operations.
 */
@Repository
public interface RatingRepository extends JpaRepository<Rating, Integer> {

    /**
     * Find all ratings for a dataset.
     */
    List<Rating> findByDataset_DatasetId(Integer datasetId);

    /**
     * Find all ratings by a user.
     */
    List<Rating> findByUser_UserId(Integer userId);

    /**
     * Find a rating by user and dataset (unique per user/dataset).
     */
    Optional<Rating> findByUser_UserIdAndDataset_DatasetId(Integer userId, Integer datasetId);

    /**
     * Check if a user has rated a dataset.
     */
    boolean existsByUser_UserIdAndDataset_DatasetId(Integer userId, Integer datasetId);

    /**
     * Calculate average rating for a dataset.
     */
    @Query("SELECT AVG(r.ratingValue) FROM Rating r WHERE r.dataset.datasetId = :datasetId")
    Double getAverageRatingForDataset(@Param("datasetId") Integer datasetId);

    /**
     * Count ratings for a dataset.
     */
    long countByDataset_DatasetId(Integer datasetId);
}
