package com.onlinedatatepo.data_repository.service;

import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetFile;
import com.onlinedatatepo.data_repository.entity.DatasetFileCategory;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.FileType;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.DatasetFileRepository;
import com.onlinedatatepo.data_repository.repository.DatasetRepository;
import com.onlinedatatepo.data_repository.repository.DatasetTableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class FileUploadService {

        private static final Set<String> ALLOWED_DATASET_TYPES = Set.of(
            "text/csv",
            "text/tab-separated-values",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

        private static final Set<String> ALLOWED_DATASET_EXTENSIONS = Set.of("csv", "tsv", "xlsx", "xls");

        private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of("pdf", "txt", "md", "doc", "docx", "rtf");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DatasetFileRepository datasetFileRepository;
    private final DatasetRepository datasetRepository;
    private final DatasetTableRepository datasetTableRepository;
    private final MetadataExtractionService metadataExtractionService;

    public FileUploadService(DatasetFileRepository datasetFileRepository, DatasetRepository datasetRepository,
                             DatasetTableRepository datasetTableRepository,
                             MetadataExtractionService metadataExtractionService) {
        this.datasetFileRepository = datasetFileRepository;
        this.datasetRepository = datasetRepository;
        this.datasetTableRepository = datasetTableRepository;
        this.metadataExtractionService = metadataExtractionService;
    }

    public DatasetFile uploadFile(MultipartFile file, Integer datasetId, User owner) throws IOException {
        validateDatasetFile(file);

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found"));

        if (!dataset.getUser().getUserId().equals(owner.getUserId())) {
            throw new IllegalArgumentException("You can only upload files to your own datasets");
        }

        // Create upload directory
        Path datasetDir = Paths.get(uploadDir, String.valueOf(datasetId));
        Files.createDirectories(datasetDir);

        // Store file with original name
        String fileName = file.getOriginalFilename();
        Path filePath = datasetDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Save metadata
        DatasetFile created = saveFileMetadata(dataset, fileName, filePath, DatasetFileCategory.DATASET);
        metadataExtractionService.triggerMetadataExtraction(created, owner);
        return created;
    }

    public DatasetFile uploadFileForNewDataset(MultipartFile file, Dataset dataset) throws IOException {
        return uploadFileForDataset(file, dataset, dataset.getUser());
    }

    public List<DatasetFile> uploadFilesForNewDataset(MultipartFile[] files, Dataset dataset) throws IOException {
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("Please upload at least one dataset file.");
        }

        List<DatasetFile> uploadedFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            uploadedFiles.add(uploadFileForDataset(file, dataset, dataset.getUser()));
        }

        if (uploadedFiles.isEmpty()) {
            throw new IllegalArgumentException("Please upload at least one valid dataset file.");
        }
        return uploadedFiles;
    }

    public DatasetFile replacePrimaryDatasetFile(MultipartFile file, Integer datasetId, User owner) throws IOException {
        validateDatasetFile(file);

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Dataset not found"));
        if (!dataset.getUser().getUserId().equals(owner.getUserId())) {
            throw new IllegalArgumentException("You can only edit files in your own datasets");
        }

        List<DatasetFile> datasetFiles = datasetFileRepository
                .findByDataset_DatasetIdAndFileCategoryOrderByUploadedAtAsc(datasetId, DatasetFileCategory.DATASET);
        if (datasetFiles.isEmpty()) {
            return uploadFileForDataset(file, dataset, owner);
        }

        DatasetFile primaryFile = datasetFiles.get(0);
        String oldPath = primaryFile.getFilePath();

        Path datasetDir = Paths.get(uploadDir, String.valueOf(datasetId));
        Files.createDirectories(datasetDir);

        String newFileName = file.getOriginalFilename();
        Path newFilePath = datasetDir.resolve(newFileName);
        Files.copy(file.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);

        primaryFile.setFileName(newFileName);
        primaryFile.setFilePath(newFilePath.toString());
        primaryFile.setFileType(getFileExtension(newFileName));
        DatasetFile savedFile = datasetFileRepository.save(primaryFile);

        List<DatasetTable> tables = datasetTableRepository.findByDataset_DatasetIdOrderByTableIdAsc(datasetId);
        if (!tables.isEmpty()) {
            DatasetTable primaryTable = tables.get(0);
            primaryTable.setTableName(newFileName);
            primaryTable.setFilePath(newFilePath.toString());
            primaryTable.setFileType(mapExtensionToFileType(savedFile.getFileType()));
            datasetTableRepository.save(primaryTable);
        }

        deleteOldFileSilently(oldPath, newFilePath.toString());
        return savedFile;
    }

    private DatasetFile uploadFileForDataset(MultipartFile file, Dataset dataset, User owner) throws IOException {
        validateDatasetFile(file);

        Path datasetDir = Paths.get(uploadDir, String.valueOf(dataset.getDatasetId()));
        Files.createDirectories(datasetDir);

        String fileName = file.getOriginalFilename();
        Path filePath = datasetDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        DatasetFile created = saveFileMetadata(dataset, fileName, filePath, DatasetFileCategory.DATASET);
        metadataExtractionService.triggerMetadataExtraction(created, owner);
        return created;
    }

    public DatasetFile uploadAdditionalDocument(MultipartFile file, Dataset dataset, DatasetFileCategory fileCategory)
            throws IOException {
        if (fileCategory == null || fileCategory == DatasetFileCategory.DATASET) {
            throw new IllegalArgumentException("Please select a valid document category");
        }

        validateAdditionalDocument(file);

        Path documentDir = Paths.get(uploadDir, String.valueOf(dataset.getDatasetId()), "documents");
        Files.createDirectories(documentDir);

        String fileName = file.getOriginalFilename();
        Path filePath = documentDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        DatasetFile existing = datasetFileRepository.findByDataset_DatasetIdAndFileCategoryAndFileName(
                dataset.getDatasetId(),
                fileCategory,
                fileName
        );
        if (existing != null) {
            existing.setFilePath(filePath.toString());
            existing.setFileType(getFileExtension(fileName));
            return datasetFileRepository.save(existing);
        }

        return saveFileMetadata(dataset, fileName, filePath, fileCategory);
    }

    public void deleteAdditionalDocument(DatasetFile file) {
        if (file == null || file.getFileCategory() == DatasetFileCategory.DATASET) {
            return;
        }

        try {
            if (file.getFilePath() != null && !file.getFilePath().isBlank()) {
                Files.deleteIfExists(Paths.get(file.getFilePath()));
            }
        } catch (IOException ignored) {
            // Best effort file cleanup.
        }

        datasetFileRepository.delete(file);
    }

    public List<DatasetFile> getFilesByDataset(Integer datasetId) {
        return datasetFileRepository.findByDataset_DatasetId(datasetId);
    }

    public List<DatasetFile> getFilesByDatasetAndCategory(Integer datasetId, DatasetFileCategory fileCategory) {
        return datasetFileRepository.findByDataset_DatasetIdAndFileCategory(datasetId, fileCategory);
    }

    private void validateDatasetFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_DATASET_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: CSV, TSV, Excel");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_DATASET_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file content type");
        }
    }

    private void validateAdditionalDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Document file is empty");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_DOCUMENT_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported document type. Allowed: PDF, TXT, MD, DOC, DOCX, RTF");
        }
    }

    private DatasetFile saveFileMetadata(Dataset dataset, String fileName, Path filePath, DatasetFileCategory category) {
        DatasetFile datasetFile = new DatasetFile();
        datasetFile.setFileName(fileName);
        datasetFile.setFilePath(filePath.toString());
        datasetFile.setFileType(getFileExtension(fileName));
        datasetFile.setFileCategory(category);
        datasetFile.setDataset(dataset);
        return datasetFileRepository.save(datasetFile);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private FileType mapExtensionToFileType(String extension) {
        if (extension == null) {
            return FileType.CSV;
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "tsv" -> FileType.TSV;
            case "xlsx", "xls" -> FileType.XLSX;
            default -> FileType.CSV;
        };
    }

    private void deleteOldFileSilently(String oldPath, String newPath) {
        if (oldPath == null || oldPath.isBlank()) {
            return;
        }
        try {
            if (oldPath.equals(newPath)) {
                return;
            }
            Files.deleteIfExists(Paths.get(oldPath));
        } catch (IOException ignored) {
            // Best effort cleanup only.
        }
    }
}
