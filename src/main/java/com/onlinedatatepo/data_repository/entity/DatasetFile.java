package com.onlinedatatepo.data_repository.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DatasetFile entity for dataset file references.
 * 
 * Maps to the "dataset_files" table in PostgreSQL.
 * Stores metadata about uploaded files associated with datasets:
 * - File name, path, and type (PDF, TXT, DOCX, etc.)
 * - Upload timestamp
 */
@Entity
@Table(name = "dataset_files")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatasetFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_category", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DatasetFileCategory fileCategory = DatasetFileCategory.DATASET;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;
}
