package com.onlinedatatepo.data_repository.service;

import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetFile;
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
import java.util.Set;

@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "text/csv",
            "text/tab-separated-values",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "tsv", "xlsx", "xls");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DatasetFileRepository datasetFileRepository;
    private final DatasetRepository datasetRepository;

    public FileUploadService(DatasetFileRepository datasetFileRepository, DatasetRepository datasetRepository) {
        this.datasetFileRepository = datasetFileRepository;
        this.datasetRepository = datasetRepository;
    }

    public DatasetFile uploadFile(MultipartFile file, Integer datasetId, User owner) throws IOException {
        validateFile(file);

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
        DatasetFile datasetFile = new DatasetFile();
        datasetFile.setFileName(fileName);
        datasetFile.setFilePath(filePath.toString());
        datasetFile.setFileType(getFileExtension(fileName));
        datasetFile.setDataset(dataset);

        return datasetFileRepository.save(datasetFile);
    }

    public DatasetFile uploadFileForNewDataset(MultipartFile file, Dataset dataset) throws IOException {
        validateFile(file);

        Path datasetDir = Paths.get(uploadDir, String.valueOf(dataset.getDatasetId()));
        Files.createDirectories(datasetDir);

        String fileName = file.getOriginalFilename();
        Path filePath = datasetDir.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        DatasetFile datasetFile = new DatasetFile();
        datasetFile.setFileName(fileName);
        datasetFile.setFilePath(filePath.toString());
        datasetFile.setFileType(getFileExtension(fileName));
        datasetFile.setDataset(dataset);

        return datasetFileRepository.save(datasetFile);
    }

    public List<DatasetFile> getFilesByDataset(Integer datasetId) {
        return datasetFileRepository.findByDataset_DatasetId(datasetId);
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type. Allowed: CSV, TSV, Excel");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
