package com.onlinedatatepo.data_repository.dto;

import java.util.List;

import com.onlinedatatepo.data_repository.entity.AccessLevel;

import jakarta.validation.constraints.NotNull;

public record DatasetPermissionUpdateRequest(
        @NotNull AccessLevel accessLevel,
        List<String> authorizedEmails
) {
}
