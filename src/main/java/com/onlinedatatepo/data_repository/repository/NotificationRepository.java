package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Notification entity.
 * Provides database access methods for notification operations.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    /**
     * Find all notifications for a user with pagination.
     */
    Page<Notification> findByUser_UserId(Integer userId, Pageable pageable);

    /**
     * Find all unread notifications for a user.
     */
    List<Notification> findByUser_UserIdAndIsReadFalse(Integer userId);

    /**
     * Find all unread notifications for a user with pagination.
     */
    Page<Notification> findByUser_UserIdAndIsReadFalse(Integer userId, Pageable pageable);

    /**
     * Count unread notifications for a user.
     */
    long countByUser_UserIdAndIsReadFalse(Integer userId);

    /**
     * Count all notifications for a user.
     */
    long countByUser_UserId(Integer userId);
}
