package com.onlinedatatepo.data_repository.dto;

import java.time.LocalDateTime;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;

public record DatasetSummaryResponse(
        Integer datasetId,
        String name,
        String description,
        String tag,
        AccessLevel accessLevel,
        String ownerName,
        String ownerEmail,
        LocalDateTime createdAt
) {
    public static DatasetSummaryResponse from(Dataset dataset) {
        return new DatasetSummaryResponse(
                dataset.getDatasetId(),
                dataset.getName(),
                dataset.getDescription(),
                dataset.getTag(),
                dataset.getAccessLevel(),
                dataset.getUser() != null ? dataset.getUser().getFullName() : null,
                dataset.getUser() != null ? dataset.getUser().getEmail() : null,
                dataset.getCreatedAt()
        );
    }
}
