package com.onlinedatatepo.data_repository.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinedatatepo.data_repository.entity.DatasetColumn;
import com.onlinedatatepo.data_repository.entity.DatasetFile;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.FileType;
import com.onlinedatatepo.data_repository.entity.MetadataExtractionStatus;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.DatasetColumnRepository;
import com.onlinedatatepo.data_repository.repository.DatasetTableRepository;

@Service
public class MetadataExtractionService {

    private final DatasetTableRepository datasetTableRepository;
    private final DatasetColumnRepository datasetColumnRepository;
    private final HttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${python-service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    public MetadataExtractionService(DatasetTableRepository datasetTableRepository,
                                     DatasetColumnRepository datasetColumnRepository) {
        this.datasetTableRepository = datasetTableRepository;
        this.datasetColumnRepository = datasetColumnRepository;
        this.httpClient = HttpClient.newHttpClient();
    }

    public void triggerMetadataExtraction(DatasetFile datasetFile, User owner) {
        if (datasetFile == null || datasetFile.getDataset() == null) {
            return;
        }

        if (datasetFile.getFileCategory() != null && datasetFile.getFileCategory().name().equals("DATASET")) {
            DatasetTable table = datasetTableRepository.findByTableNameAndDataset_DatasetId(
                    datasetFile.getFileName(), datasetFile.getDataset().getDatasetId());

            if (table == null) {
                table = new DatasetTable();
                table.setDataset(datasetFile.getDataset());
                table.setTableName(datasetFile.getFileName());
                table.setFilePath(datasetFile.getFilePath());
                table.setFileType(mapExtensionToFileType(datasetFile.getFileType()));
            }

            table.setMetadataExtractionStatus(MetadataExtractionStatus.PENDING);
            table.setMetadataExtractionError(null);
            datasetTableRepository.save(table);

            extractMetadataAsync(table, owner);
        }
    }

    public void reExtractMetadata(DatasetTable table, User owner) {
        if (table == null) {
            return;
        }
        table.setMetadataExtractionStatus(MetadataExtractionStatus.PENDING);
        table.setMetadataExtractionError(null);
        datasetTableRepository.save(table);
        extractMetadataAsync(table, owner);
    }

    private FileType mapExtensionToFileType(String extension) {
        if (extension == null) {
            return FileType.CSV;
        }

        String normalized = extension.trim().toLowerCase();
        return switch (normalized) {
            case "tsv" -> FileType.TSV;
            case "xlsx", "xls" -> FileType.XLSX;
            default -> FileType.CSV;
        };
    }

    @Async("metadataTaskExecutor")
    public void extractMetadataAsync(DatasetTable table, User owner) {
        try {
            String url = pythonServiceUrl;
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            URI uri = URI.create(url + "/extract");

            Map<String, Object> payload = new HashMap<>();
            payload.put("dataset_id", table.getDataset().getDatasetId());
            payload.put("file_path", Paths.get(table.getFilePath()).toAbsolutePath().normalize().toString());
            payload.put("file_name", table.getTableName());
            payload.put("uploaded_by", owner != null ? owner.getEmail() : "unknown");
            payload.put("uploaded_at", owner != null ? owner.getCreatedAt().toString() : null);

            String body = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                String error = String.format("Python extractor returned %d: %s", response.statusCode(), response.body());
                persistFailure(table, error);
                return;
            }

            JsonNode jsonResponse = objectMapper.readTree(response.body());
            JsonNode metadataNode = jsonResponse.path("metadata");

            if (metadataNode.isMissingNode() || metadataNode.isNull()) {
                persistFailure(table, "Metadata response missing metadata payload");
                return;
            }

            table.setMetadata(metadataNode.toString());
            table.setMetadataExtractedAt(LocalDateTime.now());
            table.setMetadataExtractionStatus(MetadataExtractionStatus.EXTRACTED);
            table.setMetadataExtractionError(null);
            datasetTableRepository.save(table);
            syncDatasetColumns(table, metadataNode);

        } catch (InterruptedException e) {
            persistFailure(table, "Metadata extraction failure: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            persistFailure(table, "Metadata extraction failure: " + e.getMessage());
        } catch (Exception e) {
            persistFailure(table, "Metadata extraction failure: " + e.getMessage());
        }
    }

    private void syncDatasetColumns(DatasetTable table, JsonNode metadataNode) {
        JsonNode columnsNode = metadataNode.path("columns");
        datasetColumnRepository.deleteByTable_TableId(table.getTableId());

        if (!columnsNode.isArray()) {
            return;
        }

        for (JsonNode columnNode : columnsNode) {
            String columnName = columnNode.path("name").asText("").trim();
            if (columnName.isBlank()) {
                continue;
            }

            String columnType = columnNode.path("type").asText("STRING").trim();
            if (columnType.isBlank()) {
                columnType = "STRING";
            }

            DatasetColumn datasetColumn = new DatasetColumn();
            datasetColumn.setTable(table);
            datasetColumn.setColumnName(columnName);
            datasetColumn.setColumnType(columnType.toUpperCase());
            datasetColumnRepository.save(datasetColumn);
        }
    }

    private void persistFailure(DatasetTable table, String errorMsg) {
        table.setMetadataExtractionStatus(MetadataExtractionStatus.FAILED);
        table.setMetadataExtractionError(errorMsg);
        table.setMetadataExtractedAt(LocalDateTime.now());
        datasetTableRepository.save(table);
    }
}
