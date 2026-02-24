package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TableVersion entity for linking tables to specific dataset versions.
 * 
 * Maps to the "table_version" table in PostgreSQL.
 * Junction table with composite key (table_id, version_id).
 * Tracks which tables were present in which version of a dataset.
 */
@Entity
@Table(
    name = "table_version",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"table_id", "version_id"},
        name = "uq_table_version"
    )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_version_id")
    private Integer tableVersionId;

    @Column(name = "table_version_no", nullable = false)
    @NotNull(message = "Table version number cannot be null")
    private Integer tableVersionNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "table_id", nullable = false)
    private DatasetTable table;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "version_id", nullable = false)
    private Version version;
}
