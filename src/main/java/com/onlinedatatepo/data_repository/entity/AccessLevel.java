package com.onlinedatatepo.data_repository.entity;

/**
 * Dataset access level enumeration.
 * Controls dataset visibility and access permissions.
 */
public enum AccessLevel {
    PRIVATE,      // Only owner can access
    PUBLIC,       // Everyone can access
    AUTHORIZED    // Only authorized users can access
}
