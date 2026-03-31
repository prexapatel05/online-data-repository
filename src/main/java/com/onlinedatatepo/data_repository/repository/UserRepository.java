package com.onlinedatatepo.data_repository.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.onlinedatatepo.data_repository.entity.User;

/**
 * Repository for User entity.
 * Provides database access methods for user operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * Find a user by username.
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists by username.
     */
    boolean existsByUsername(String username);

    /**
     * Check if a user exists by email.
     */
    boolean existsByEmail(String email);

    Page<User> findAllByOrderByFullNameAsc(Pageable pageable);
}
