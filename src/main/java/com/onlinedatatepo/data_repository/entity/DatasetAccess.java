package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DatasetAccess entity for authorized user access control.
 * 
 * Maps to the "dataset_access" table in PostgreSQL.
 * Junction table with composite primary key (user_id, dataset_id).
 * Represents users who have been authorized to access a dataset
 * (used when dataset access_level = AUTHORIZED).
 */
@Entity
@Table(name = "dataset_access")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetAccess {

    @EmbeddedId
    private DatasetAccessId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("datasetId")
    @JoinColumn(name = "dataset_id")
    private Dataset dataset;
}
