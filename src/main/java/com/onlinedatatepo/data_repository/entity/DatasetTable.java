package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DatasetTable entity for storing table metadata.
 * 
 * Maps to the "dataset_tables" table in PostgreSQL.
 * Represents each table/file in a dataset:
 * - Contains column definitions (schema information)
 * - Stores file path and file type (CSV, TSV, XLSX)
 * - Part of dataset versioning
 */
@Entity
@Table(name = "dataset_tables")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Integer tableId;

    @Column(name = "table_name", nullable = false, length = 255)
    @NotBlank(message = "Table name cannot be blank")
    private String tableName;

    @Column(name = "file_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Column(name = "file_path", nullable = false, length = 500)
    @NotBlank(message = "File path cannot be blank")
    private String filePath;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DatasetColumn> columns;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TableVersion> tableVersions;
}
