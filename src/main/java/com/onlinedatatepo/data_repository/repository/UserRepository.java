package com.onlinedatatepo.data_repository.repository;

import com.onlinedatatepo.data_repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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
}
