package com.onlinedatatepo.data_repository.dto;

import java.util.List;

import com.onlinedatatepo.data_repository.entity.AccessLevel;

public record DatasetPermissionUpdateResponse(
        Integer datasetId,
        AccessLevel accessLevel,
        List<String> authorizedEmails,
        List<String> invalidEmails,
        String message
) {
}
