package com.onlinedatatepo.data_repository.service;

import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetFile;
import com.onlinedatatepo.data_repository.entity.DatasetFileCategory;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.DatasetFileRepository;
import com.onlinedatatepo.data_repository.repository.DatasetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
    private final MetadataExtractionService metadataExtractionService;

    public FileUploadService(DatasetFileRepository datasetFileRepository, DatasetRepository datasetRepository,
                             MetadataExtractionService metadataExtractionService) {
        this.datasetFileRepository = datasetFileRepository;
        this.datasetRepository = datasetRepository;
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
        validateDatasetFile(file);

        Path datasetDir = Paths.get(uploadDir, String.valueOf(dataset.getDatasetId()));
        Files.createDirectories(datasetDir);

        String fileName = file.getOriginalFilename();
        Path filePath = datasetDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        DatasetFile created = saveFileMetadata(dataset, fileName, filePath, DatasetFileCategory.DATASET);
        metadataExtractionService.triggerMetadataExtraction(created, dataset.getUser());
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

        return saveFileMetadata(dataset, fileName, filePath, fileCategory);
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
}
