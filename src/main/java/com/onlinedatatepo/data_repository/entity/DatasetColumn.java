package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DatasetColumn entity for storing column metadata.
 * 
 * Maps to the "dataset_columns" table in PostgreSQL.
 * Stores schema information for each column in a dataset table:
 * - Column name
 * - Column data type (INT, VARCHAR, TIMESTAMP, etc.)
 * - Part of dataset schema metadata
 */
@Entity
@Table(name = "dataset_columns")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetColumn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "column_id")
    private Integer columnId;

    @Column(name = "column_name", nullable = false, length = 255)
    @NotBlank(message = "Column name cannot be blank")
    private String columnName;

    @Column(name = "column_type", nullable = false, length = 100)
    @NotBlank(message = "Column type cannot be blank")
    private String columnType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "table_id", nullable = false)
    private DatasetTable table;
}
