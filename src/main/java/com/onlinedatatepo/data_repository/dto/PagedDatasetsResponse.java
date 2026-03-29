package com.onlinedatatepo.data_repository.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.onlinedatatepo.data_repository.entity.Dataset;

public record PagedDatasetsResponse(
        List<DatasetSummaryResponse> items,
        int page,
        int size,
        int totalPages,
        long totalItems
) {
    public static PagedDatasetsResponse from(Page<Dataset> datasets) {
        return new PagedDatasetsResponse(
                datasets.getContent().stream().map(DatasetSummaryResponse::from).toList(),
                datasets.getNumber(),
                datasets.getSize(),
                datasets.getTotalPages(),
                datasets.getTotalElements()
        );
    }
}
