package com.onlinedatatepo.data_repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;
}
