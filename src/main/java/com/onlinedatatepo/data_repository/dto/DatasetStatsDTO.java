package com.onlinedatatepo.data_repository.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for dataset statistics and metadata used in dashboard and list views.
 * Optimized to reduce N+1 queries by embedding stats directly with datasets.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetStatsDTO {
    private Integer datasetId;
    private long viewCount;
    private long downloadCount;
    private String fileType;  // CSV, JSON, etc.
}
