package com.onlinedatatepo.data_repository.controller;

import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.DatasetTableRepository;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import com.onlinedatatepo.data_repository.service.MetadataExtractionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class MetadataController {

    private final DatasetService datasetService;
    private final DatasetTableRepository datasetTableRepository;
    private final MetadataExtractionService metadataExtractionService;
    private final AuthService authService;

    public MetadataController(DatasetService datasetService,
                              DatasetTableRepository datasetTableRepository,
                              MetadataExtractionService metadataExtractionService,
                              AuthService authService) {
        this.datasetService = datasetService;
        this.datasetTableRepository = datasetTableRepository;
        this.metadataExtractionService = metadataExtractionService;
        this.authService = authService;
    }

    @PostMapping("/datasets/{datasetId}/tables/{tableId}/re-extract-metadata")
    public ResponseEntity<?> reExtractMetadata(@PathVariable Integer datasetId,
                                               @PathVariable Integer tableId,
                                               Authentication authentication) {
        User currentUser = authService.findByEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Optional<Dataset> datasetOptional = datasetService.findById(datasetId);
        if (datasetOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dataset not found");
        }

        Dataset dataset = datasetOptional.get();
        if (!dataset.getUser().getUserId().equals(currentUser.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Forbidden");
        }

        Optional<DatasetTable> tableOptional = datasetTableRepository.findById(tableId);
        if (tableOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Dataset table not found");
        }

        DatasetTable table = tableOptional.get();
        if (!table.getDataset().getDatasetId().equals(datasetId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Table does not belong to dataset");
        }

        metadataExtractionService.reExtractMetadata(table, currentUser);

        return ResponseEntity.accepted().body("Metadata re-extraction started");
    }
}
