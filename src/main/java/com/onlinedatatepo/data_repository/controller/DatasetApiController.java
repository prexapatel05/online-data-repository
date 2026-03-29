package com.onlinedatatepo.data_repository.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.onlinedatatepo.data_repository.dto.DatasetPermissionUpdateRequest;
import com.onlinedatatepo.data_repository.dto.DatasetPermissionUpdateResponse;
import com.onlinedatatepo.data_repository.dto.PagedDatasetsResponse;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/datasets")
@Validated
public class DatasetApiController {

    private final DatasetService datasetService;
    private final AuthService authService;

    public DatasetApiController(DatasetService datasetService, AuthService authService) {
        this.datasetService = datasetService;
        this.authService = authService;
    }

    @GetMapping("/my")
    public ResponseEntity<?> myDatasets(@RequestParam(value = "page", defaultValue = "0") int page,
                                        @RequestParam(value = "size", defaultValue = "12") int size,
                                        org.springframework.security.core.Authentication authentication) {
        User currentUser = authService.findByEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Dataset> datasets = datasetService.getDatasetsByUser(currentUser.getUserId(), pageable);
        return ResponseEntity.ok(PagedDatasetsResponse.from(datasets));
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<?> sharedWithMe(@RequestParam(value = "page", defaultValue = "0") int page,
                                          @RequestParam(value = "size", defaultValue = "12") int size,
                                          org.springframework.security.core.Authentication authentication) {
        User currentUser = authService.findByEmail(authentication.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Dataset> datasets = datasetService.getSharedWithMeDatasets(currentUser.getUserId(), pageable);
        return ResponseEntity.ok(PagedDatasetsResponse.from(datasets));
    }

    @PutMapping("/{datasetId}/permissions")
    public ResponseEntity<?> updatePermissions(@PathVariable Integer datasetId,
                                               @Valid @RequestBody DatasetPermissionUpdateRequest request,
                                               org.springframework.security.core.Authentication authentication) {
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

        List<String> authorizedEmails = request.authorizedEmails() == null
                ? Collections.emptyList()
                : request.authorizedEmails();

        DatasetService.PermissionUpdateResult result = datasetService.updatePermissionsByEmails(
                dataset,
                request.accessLevel(),
                authorizedEmails
        );

        DatasetPermissionUpdateResponse response = new DatasetPermissionUpdateResponse(
                datasetId,
                result.dataset().getAccessLevel(),
                result.addedEmails(),
                result.invalidEmails(),
                result.invalidEmails().isEmpty()
                        ? "Permissions updated successfully"
                        : "Permissions updated with some invalid emails skipped"
        );

        return ResponseEntity.ok(response);
    }
}
