package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite key for DatasetAccess entity.
 * Embeds user_id and dataset_id as a single primary key.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetAccessId implements Serializable {
    private Integer userId;
    private Integer datasetId;
}
