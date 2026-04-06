package com.onlinedatatepo.data_repository.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.UriUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinedatatepo.data_repository.config.DatasetTagCatalog;
import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.AuditLog;
import com.onlinedatatepo.data_repository.entity.Comment;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetColumn;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.FileType;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.AuditLogRepository;
import com.onlinedatatepo.data_repository.repository.CommentRepository;
import com.onlinedatatepo.data_repository.repository.DatasetColumnRepository;
import com.onlinedatatepo.data_repository.repository.UserRepository;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.CommentService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import com.onlinedatatepo.data_repository.service.QueryExecutionService;
import com.onlinedatatepo.data_repository.service.RatingService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class DataExplorerController {

    private static final int QUERY_PAGE_SIZE = 50;
    private static final int EXPORT_MAX_ROWS = 5000;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private final DatasetService datasetService;
    private final AuthService authService;
    private final AuditLogRepository auditLogRepository;
    private final CommentRepository commentRepository;
    private final DatasetColumnRepository datasetColumnRepository;
    private final QueryExecutionService queryExecutionService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final CommentService commentService;
    private final RatingService ratingService;

    public DataExplorerController(DatasetService datasetService,
                                  AuthService authService,
                                  AuditLogRepository auditLogRepository,
                                  CommentRepository commentRepository,
                                  DatasetColumnRepository datasetColumnRepository,
                                  QueryExecutionService queryExecutionService,
                                  UserRepository userRepository,
                                  ObjectMapper objectMapper,
                                  CommentService commentService,
                                  RatingService ratingService) {
        this.datasetService = datasetService;
        this.authService = authService;
        this.auditLogRepository = auditLogRepository;
        this.commentRepository = commentRepository;
        this.datasetColumnRepository = datasetColumnRepository;
        this.queryExecutionService = queryExecutionService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.commentService = commentService;
        this.ratingService = ratingService;
    }

    @GetMapping("/datasets")
    public String datasetsPage(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "ownerId", required = false) Integer ownerId,
                               @RequestParam(value = "visibility", required = false) String visibility,
                               @RequestParam(value = "format", required = false) String format,
                               @RequestParam(value = "sort", defaultValue = "recent") String sort,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "12") int size,
                               org.springframework.security.core.Authentication authentication,
                               Model model) {
        User currentUser = authService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);

        AccessLevel visibilityFilter = parseVisibility(visibility);
        FileType fileTypeFilter = parseFileType(format);
        String selectedSort = normalizeDatasetSort(sort);

        Pageable pageable;
        Page<Dataset> datasets;

        switch (selectedSort) {
            case "alpha" -> {
            pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Order.asc("name"), Sort.Order.desc("createdAt")));
            datasets = datasetService.searchAccessibleDatasets(
                currentUser.getUserId(),
                search,
                category,
                ownerId,
                visibilityFilter,
                fileTypeFilter,
                pageable
            );
            }
            case "rating" -> {
            pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            datasets = datasetService.searchAccessibleDatasetsOrderByRating(
                currentUser.getUserId(),
                search,
                category,
                ownerId,
                visibilityFilter,
                fileTypeFilter,
                pageable
            );
            }
            case "views" -> {
            pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            datasets = datasetService.searchAccessibleDatasetsOrderByViews(
                currentUser.getUserId(),
                search,
                category,
                ownerId,
                visibilityFilter,
                fileTypeFilter,
                pageable
            );
            }
            default -> {
            pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt"));
            datasets = datasetService.searchAccessibleDatasets(
                currentUser.getUserId(),
                search,
                category,
                ownerId,
                visibilityFilter,
                fileTypeFilter,
                pageable
            );
            }
        }

        // Get stats for each dataset
        List<Integer> datasetIds = datasets.getContent().stream()
                .map(Dataset::getDatasetId)
                .toList();
        var datasetStatsMap = datasetService.getDatasetStatsMap(datasetIds);

        model.addAttribute("datasets", datasets.getContent());
        model.addAttribute("datasetStats", datasetStatsMap);
        model.addAttribute("totalPages", datasets.getTotalPages());
        model.addAttribute("currentPage", datasets.getNumber());
        model.addAttribute("totalItems", datasets.getTotalElements());

        model.addAttribute("search", safeValue(search));
        model.addAttribute("category", safeValue(category));
        model.addAttribute("ownerId", ownerId);
        model.addAttribute("visibility", visibilityFilter != null ? visibilityFilter.name() : "");
        model.addAttribute("format", fileTypeFilter != null ? fileTypeFilter.name() : "");
        model.addAttribute("sort", selectedSort);
        model.addAttribute("browseCategories", DatasetTagCatalog.TAGS);
        List<User> owners = userRepository.findAllByOrderByFullNameAsc(PageRequest.of(0, 200)).getContent();
        model.addAttribute("owners", owners);
        model.addAttribute("selectedOwnerLabel", ownerId == null
            ? ""
            : owners.stream()
                .filter(owner -> owner.getUserId() != null && owner.getUserId().equals(ownerId))
                .findFirst()
                .map(this::ownerSearchLabel)
                .orElse(""));
        model.addAttribute("hasDatasetFilters",
            (search != null && !search.isBlank())
            || (category != null && !category.isBlank())
            || ownerId != null
            || visibilityFilter != null
            || fileTypeFilter != null);

        return "datasets";
    }

    private String normalizeDatasetSort(String sort) {
        if (sort == null) {
            return "recent";
        }
        String normalized = sort.trim().toLowerCase();
        return switch (normalized) {
            case "views", "alpha", "rating", "recent" -> normalized;
            default -> "recent";
        };
    }

    @GetMapping("/datasets/{datasetId}")
    @org.springframework.transaction.annotation.Transactional
    public String datasetDetailPage(@PathVariable Integer datasetId,
                                    @RequestParam(value = "tab", defaultValue = "overview") String tab,
                                    org.springframework.security.core.Authentication authentication,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        User currentUser = authService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);

        Optional<Dataset> datasetOptional = datasetService.findById(datasetId);
        if (datasetOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Dataset not found.");
            return "redirect:/datasets";
        }

        Dataset dataset = datasetOptional.get();
        if (!datasetService.canAccessDataset(currentUser, dataset)) {
            redirectAttributes.addFlashAttribute("error", "You do not have access to this dataset.");
            return "redirect:/datasets";
        }

        List<DatasetTable> tables = datasetService.getTablesByDatasetId(datasetId);
        DatasetTable primaryTable = tables.isEmpty() ? null : tables.get(0);
        List<DatasetColumn> columns = primaryTable == null
                ? Collections.emptyList()
                : datasetColumnRepository.findByTable_TableId(primaryTable.getTableId());

        List<Map<String, String>> previewRows = Collections.emptyList();
        if (primaryTable != null) {
            try {
                previewRows = queryExecutionService.previewRows(primaryTable, 25);
            } catch (IllegalArgumentException ignored) {
                previewRows = Collections.emptyList();
            }
        }

        List<Map<String, Object>> tablePreviews = buildTablePreviews(tables);

        long totalRows = 0;
        if (primaryTable != null) {
            try {
                totalRows = queryExecutionService.executeSelectProjection(
                        primaryTable,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0,
                        1
                ).totalMatchedRows();
            } catch (IllegalArgumentException ignored) {
                totalRows = 0;
            }
        }

        List<AuditLog> datasetActivity = auditLogRepository.searchLogs(
                null,
                null,
                null,
                null,
                datasetId,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();

        Map<String, Object> parsedMetadata = parseTableMetadata(primaryTable);
        List<Map<String, Object>> metadataColumns = extractMetadataColumns(parsedMetadata);
        Map<String, Object> dimensions = asObjectMap(parsedMetadata.get("dimensions"));
        Map<String, Object> qualityMetrics = asObjectMap(parsedMetadata.get("quality_metrics"));
        Map<String, Object> fileInfo = asObjectMap(parsedMetadata.get("file_info"));

        long metadataRowCount = toLong(dimensions.get("row_count"), totalRows);
        long metadataColumnCount = toLong(dimensions.get("column_count"), columns.size());
        String metadataCompleteness = formatPercent(qualityMetrics.get("completeness"));

        Map<String, Object> statistics = buildDatasetStatistics(columns, previewRows, totalRows, metadataColumnCount);
        statistics.put("viewCount", auditLogRepository.countByDataset_DatasetIdAndActionIgnoreCase(datasetId, "DATASET_VIEWED"));
        statistics.put("downloadCount", auditLogRepository.countByDataset_DatasetIdAndActionIgnoreCase(datasetId, "DATASET_DOWNLOADED"));

        String metadataTag = (dataset.getTag() == null || dataset.getTag().isBlank())
                ? "General"
                : dataset.getTag();
        if ("General".equals(metadataTag) && fileInfo.get("file_type") != null) {
            metadataTag = String.valueOf(fileInfo.get("file_type")).toUpperCase();
        }

        List<Map<String, String>> metadataGeneralEntries = buildGeneralMetadataEntries(parsedMetadata);
        List<Map<String, Object>> tableDetails = buildTableDetails(tables);

        String ownerDisplayName = defaultUserDisplayName(dataset.getUser());
        String datasetFormat = primaryTable != null && primaryTable.getFileType() != null
            ? primaryTable.getFileType().name()
            : "N/A";
        String datasetSize = primaryTable != null ? resolveReadableFileSize(primaryTable.getFilePath()) : "N/A";
        List<Comment> topLevelComments = commentService.getTopLevelComments(datasetId);
        Long commentCount = commentService.countComments(datasetId);
        Double averageRating = ratingService.averageRating(datasetId);
        Long ratingCount = ratingService.ratingCount(datasetId);
        Integer userRating = ratingService.currentUserRating(currentUser.getUserId(), datasetId);

        model.addAttribute("dataset", dataset);
        model.addAttribute("tab", normalizeTab(tab));
        model.addAttribute("tables", tables);
        model.addAttribute("columns", columns);
        model.addAttribute("previewRows", previewRows);
        model.addAttribute("tablePreviews", tablePreviews);
        model.addAttribute("datasetActivity", datasetActivity);
        model.addAttribute("statistics", statistics);
        model.addAttribute("authorizedUsers", dataset.getAuthorizedUsers() == null ? Collections.emptyList() : dataset.getAuthorizedUsers());
        model.addAttribute("ownerDisplayName", ownerDisplayName);
        model.addAttribute("datasetFormat", datasetFormat);
        model.addAttribute("datasetSize", datasetSize);
        model.addAttribute("isOwner", dataset.getUser().getUserId().equals(currentUser.getUserId()));
        model.addAttribute("metadataColumns", metadataColumns);
        model.addAttribute("metadataTag", metadataTag);
        model.addAttribute("metadataRowCount", metadataRowCount);
        model.addAttribute("metadataColumnCount", metadataColumnCount);
        model.addAttribute("metadataCompleteness", metadataCompleteness);
        model.addAttribute("metadataGeneralEntries", metadataGeneralEntries);
        model.addAttribute("tableDetails", tableDetails);
        model.addAttribute("hasMetadata", !metadataColumns.isEmpty());
        model.addAttribute("comments", topLevelComments);
        model.addAttribute("commentCount", commentCount);
        model.addAttribute("averageRating", averageRating == null ? null : String.format("%.1f", averageRating));
        model.addAttribute("ratingCount", ratingCount);
        model.addAttribute("userRating", userRating);

        if ("overview".equals(normalizeTab(tab))) {
            createAuditLog(currentUser, dataset, "DATASET_VIEWED", "Viewed dataset details (tab: overview).");
        }

        return "dataset-detail";
    }

    @PostMapping("/datasets/{datasetId}/comments")
    public String addComment(@PathVariable Integer datasetId,
                             @RequestParam("content") String content,
                             @RequestParam(value = "parentId", required = false) Integer parentId,
                             org.springframework.security.core.Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User currentUser = authService.findByEmail(authentication.getName());
        try {
            Comment saved = commentService.addComment(currentUser, datasetId, parentId, content);
            if (parentId == null) {
                createAuditLog(currentUser, saved.getDataset(), "COMMENT_CREATED", "Added a comment on dataset discussion.");
            } else {
                createAuditLog(currentUser, saved.getDataset(), "COMMENT_REPLIED", "Replied to a dataset discussion comment.");
            }
            redirectAttributes.addFlashAttribute("success", "Comment posted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/datasets/" + datasetId + "?tab=discussion";
    }

    @PostMapping("/datasets/{datasetId}/ratings")
    public String rateDataset(@PathVariable Integer datasetId,
                              @RequestParam("ratingValue") Integer ratingValue,
                              org.springframework.security.core.Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User currentUser = authService.findByEmail(authentication.getName());
        try {
            var savedRating = ratingService.rateDataset(currentUser, datasetId, ratingValue == null ? 0 : ratingValue);
            createAuditLog(currentUser, savedRating.getDataset(), "DATASET_RATED", "Submitted dataset rating.");
            redirectAttributes.addFlashAttribute("success", "Rating submitted.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/datasets/" + datasetId + "?tab=discussion";
    }

    @GetMapping("/datasets/{datasetId}/comments/{parentId}/nested")
    @ResponseBody
    public ResponseEntity<?> getNestedComments(@PathVariable Integer datasetId,
                                               @PathVariable Integer parentId,
                                               @RequestParam(value = "limit", defaultValue = "20") int limit,
                                               org.springframework.security.core.Authentication authentication) {
        User currentUser = authService.findByEmail(authentication.getName());

        Optional<Dataset> datasetOptional = datasetService.findById(datasetId);
        if (datasetOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dataset dataset = datasetOptional.get();
        if (!datasetService.canAccessDataset(currentUser, dataset)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Find parent comment
        Optional<Comment> parentOpt = commentRepository.findById(parentId);
        if (parentOpt.isEmpty() || !parentId.equals(parentOpt.get().getCommentId())) {
            return ResponseEntity.notFound().build();
        }

        Comment parent = parentOpt.get();
        // Verify parent belongs to this dataset
        if (parent.getDataset() == null || !datasetId.equals(parent.getDataset().getDatasetId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Get nested replies
        List<Comment> nestedReplies = parent.getReplies() != null 
            ? parent.getReplies().stream().limit(limit).collect(Collectors.toList())
            : Collections.emptyList();

        // Build response DTO
        List<Map<String, Object>> response = nestedReplies.stream().map(comment -> {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("commentId", comment.getCommentId());
            dto.put("content", comment.getContent());
            dto.put("authorName", comment.getUser() != null 
                ? (comment.getUser().getFullName() != null ? comment.getUser().getFullName() : comment.getUser().getUsername())
                : "Anonymous");
            dto.put("createdAt", comment.getCreatedAt() != null 
                ? comment.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
                : "Unknown");
            dto.put("replyCount", comment.getReplies() != null ? comment.getReplies().size() : 0);
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/datasets/{datasetId}/download")
    public ResponseEntity<Resource> downloadDataset(@PathVariable Integer datasetId,
                                                    org.springframework.security.core.Authentication authentication,
                                                    RedirectAttributes redirectAttributes) {
        User currentUser = authService.findByEmail(authentication.getName());

        Optional<Dataset> datasetOptional = datasetService.findById(datasetId);
        if (datasetOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Dataset dataset = datasetOptional.get();
        if (!datasetService.canAccessDataset(currentUser, dataset)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<DatasetTable> tables = datasetService.getTablesByDatasetId(datasetId);
        if (tables.isEmpty() || tables.get(0).getFilePath() == null || tables.get(0).getFilePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        DatasetTable primaryTable = tables.get(0);
        try {
            Path filePath = Paths.get(primaryTable.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String fileName = filePath.getFileName() != null ? filePath.getFileName().toString() : "dataset-file";
            String encodedFileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            createAuditLog(currentUser, dataset, "DATASET_DOWNLOADED", "Downloaded dataset file: " + fileName);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(resource);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Unable to download dataset file.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/datasets/{datasetId}/delete")
    public String deleteDataset(@PathVariable Integer datasetId,
                                org.springframework.security.core.Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User currentUser = authService.findByEmail(authentication.getName());

        Optional<Dataset> datasetOptional = datasetService.findById(datasetId);
        if (datasetOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Dataset not found.");
            return "redirect:/datasets";
        }

        Dataset dataset = datasetOptional.get();
        if (!dataset.getUser().getUserId().equals(currentUser.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "Only the owner can delete this dataset.");
            return "redirect:/datasets/" + datasetId;
        }

        String deletedName = dataset.getName();
        datasetService.deleteDataset(dataset);
        deleteDatasetFiles(datasetId);

        redirectAttributes.addFlashAttribute("success", "Dataset '" + deletedName + "' deleted successfully.");
        return "redirect:/my-datasets";
    }

    @GetMapping("/query-builder")
    public String queryBuilderPage(@RequestParam(value = "datasetId", required = false) Integer datasetId,
                                   org.springframework.security.core.Authentication authentication,
                                   Model model) {
        User currentUser = authService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);

        List<Dataset> datasets = datasetService.searchAccessibleDatasets(
                currentUser.getUserId(),
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDatasetId", datasetId);
        model.addAttribute("filters", Collections.singletonList(new QueryExecutionService.QueryFilter("", "=", "")));
        model.addAttribute("selectedColumns", Collections.emptyList());

        if (datasetId != null) {
            DatasetTable table = resolvePrimaryTable(datasetId, currentUser, model);
            if (table != null) {
                List<String> availableColumns = queryExecutionService.executeSelectProjection(
                        table,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0,
                        1
                ).columns();
                model.addAttribute("availableColumns", availableColumns);
            }
        }

        return "query-builder";
    }

    @PostMapping("/query-builder/execute")
    public String executeQuery(@RequestParam("datasetId") Integer datasetId,
                               @RequestParam(value = "selectedColumns", required = false) List<String> selectedColumns,
                               @RequestParam(value = "filterColumn", required = false) List<String> filterColumns,
                               @RequestParam(value = "filterOperator", required = false) List<String> filterOperators,
                               @RequestParam(value = "filterValue", required = false) List<String> filterValues,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               org.springframework.security.core.Authentication authentication,
                               Model model) {
        User currentUser = authService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);

        List<Dataset> datasets = datasetService.searchAccessibleDatasets(
                currentUser.getUserId(),
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();
        model.addAttribute("datasets", datasets);
        model.addAttribute("selectedDatasetId", datasetId);

        DatasetTable table = resolvePrimaryTable(datasetId, currentUser, model);
        if (table == null) {
            return "query-builder";
        }

        List<QueryExecutionService.QueryFilter> filters = buildFilters(filterColumns, filterOperators, filterValues);
        model.addAttribute("filters", filters.isEmpty()
                ? Collections.singletonList(new QueryExecutionService.QueryFilter("", "=", ""))
                : filters);
        model.addAttribute("selectedColumns", selectedColumns == null ? Collections.emptyList() : selectedColumns);

        try {
            QueryExecutionService.QueryResult result = queryExecutionService.executeSelectProjection(
                    table,
                    selectedColumns == null ? Collections.emptyList() : selectedColumns,
                    filters,
                    Math.max(page, 0),
                    QUERY_PAGE_SIZE
            );

            model.addAttribute("availableColumns", result.columns());
            model.addAttribute("result", result);
            model.addAttribute("queryPage", Math.max(page, 0));
            model.addAttribute("queryTotalPages", (int) Math.ceil((double) result.totalMatchedRows() / QUERY_PAGE_SIZE));

            createAuditLog(currentUser, table.getDataset(), "QUERY_EXECUTED", "Executed select/projection query on dataset.");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
        }

        return "query-builder";
    }

    @PostMapping("/query-builder/export")
    public void exportQuery(@RequestParam("datasetId") Integer datasetId,
                            @RequestParam(value = "selectedColumns", required = false) List<String> selectedColumns,
                            @RequestParam(value = "filterColumn", required = false) List<String> filterColumns,
                            @RequestParam(value = "filterOperator", required = false) List<String> filterOperators,
                            @RequestParam(value = "filterValue", required = false) List<String> filterValues,
                            @RequestParam(value = "exportFormat", defaultValue = "csv") String exportFormat,
                            org.springframework.security.core.Authentication authentication,
                            HttpServletResponse response) throws Exception {
        User currentUser = authService.findByEmail(authentication.getName());
        DatasetTable table = resolvePrimaryTable(datasetId, currentUser, null);
        if (table == null) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            return;
        }

        String normalizedFormat = exportFormat == null ? "csv" : exportFormat.trim().toLowerCase();
        if (!"csv".equals(normalizedFormat) && !"tsv".equals(normalizedFormat)) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Unsupported export format. Use csv or tsv.");
            return;
        }

        List<QueryExecutionService.QueryFilter> filters = buildFilters(filterColumns, filterOperators, filterValues);
        QueryExecutionService.QueryResult result = queryExecutionService.executeSelectProjectionForExport(
                table,
                selectedColumns == null ? Collections.emptyList() : selectedColumns,
                filters,
                EXPORT_MAX_ROWS
        );

        char delimiter = "tsv".equals(normalizedFormat) ? '\t' : ',';
        String contentType = "tsv".equals(normalizedFormat) ? "text/tab-separated-values" : "text/csv";
        String fileName = "query-result." + normalizedFormat;

        response.setContentType(contentType);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

        response.getWriter().write(joinRow(result.columns(), delimiter));
        response.getWriter().write("\n");
        for (Map<String, String> row : result.rows()) {
            List<String> values = new ArrayList<>();
            for (String column : result.columns()) {
                values.add(escapeDelimitedValue(row.getOrDefault(column, ""), delimiter));
            }
            response.getWriter().write(joinRow(values, delimiter));
            response.getWriter().write("\n");
        }

        createAuditLog(currentUser, table.getDataset(), "QUERY_EXPORTED", "Exported query result in " + normalizedFormat.toUpperCase() + " format.");
    }

    private DatasetTable resolvePrimaryTable(Integer datasetId, User currentUser, Model model) {
        Optional<Dataset> datasetOptional = datasetService.findById(datasetId);
        if (datasetOptional.isEmpty() || !datasetService.canAccessDataset(currentUser, datasetOptional.get())) {
            if (model != null) {
                model.addAttribute("error", "Dataset not found or access denied.");
            }
            return null;
        }

        List<DatasetTable> tables = datasetService.getTablesByDatasetId(datasetId);
        if (tables.isEmpty()) {
            if (model != null) {
                model.addAttribute("error", "No queryable table found for this dataset yet.");
            }
            return null;
        }
        return tables.get(0);
    }

    private List<QueryExecutionService.QueryFilter> buildFilters(List<String> columns,
                                                                 List<String> operators,
                                                                 List<String> values) {
        if (columns == null || operators == null || values == null) {
            return Collections.emptyList();
        }

        int filterCount = Math.min(columns.size(), Math.min(operators.size(), values.size()));
        List<QueryExecutionService.QueryFilter> filters = new ArrayList<>();

        for (int i = 0; i < filterCount; i++) {
            String column = columns.get(i);
            String operator = operators.get(i);
            String value = values.get(i);

            if ((column == null || column.isBlank()) && (value == null || value.isBlank())) {
                continue;
            }

            filters.add(new QueryExecutionService.QueryFilter(column, operator, value));
        }

        return filters;
    }

    private Map<String, Object> buildDatasetStatistics(List<DatasetColumn> columns,
                                                       List<Map<String, String>> previewRows,
                                                       long totalRows,
                                                       long metadataColumnCount) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalColumns", metadataColumnCount > 0 ? metadataColumnCount : columns.size());
        stats.put("previewRows", previewRows.size());
        stats.put("totalRows", totalRows);

        int missingValues = 0;
        for (Map<String, String> row : previewRows) {
            for (String value : row.values()) {
                if (value == null || value.isBlank()) {
                    missingValues++;
                }
            }
        }
        stats.put("missingValues", missingValues);
        return stats;
    }

    private Map<String, Object> parseTableMetadata(DatasetTable table) {
        if (table == null || table.getMetadata() == null || table.getMetadata().isBlank()) {
            return Collections.emptyMap();
        }

        try {
            return objectMapper.readValue(table.getMetadata(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMetadataColumns(Map<String, Object> metadata) {
        Object rawColumns = metadata.get("columns");
        if (!(rawColumns instanceof List<?> rawList)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> columns = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> rawMap)) {
                continue;
            }

            Map<String, Object> normalized = new LinkedHashMap<>();
            Object rawName = rawMap.get("name");
            Object rawType = rawMap.get("type");
            String name = rawName == null ? "" : String.valueOf(rawName);
            String type = rawType == null ? "STRING" : String.valueOf(rawType).toUpperCase();

            List<String> sampleValues = new ArrayList<>();
            Object rawSamples = rawMap.get("sample_values");
            if (rawSamples instanceof List<?> sampleList) {
                for (Object sample : sampleList) {
                    if (sample != null) {
                        sampleValues.add(String.valueOf(sample));
                    }
                }
            }

            String minValue = "-";
            String maxValue = "-";
            Object numericRange = rawMap.get("numeric_range");
            if (numericRange instanceof Map<?, ?> rangeMap) {
                Object min = rangeMap.get("min");
                Object max = rangeMap.get("max");
                if (min != null) {
                    minValue = String.valueOf(min);
                }
                if (max != null) {
                    maxValue = String.valueOf(max);
                }
            } else if (!sampleValues.isEmpty()) {
                minValue = sampleValues.get(0);
                maxValue = sampleValues.get(sampleValues.size() - 1);
            }

            normalized.put("name", name);
            normalized.put("type", type);
            normalized.put("sampleValues", sampleValues);
            normalized.put("minValue", minValue);
            normalized.put("maxValue", maxValue);
            columns.add(normalized);
        }

        return columns;
    }

    private List<String> splitTags(String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            return Collections.emptyList();
        }

        return java.util.Arrays.stream(rawTag.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private Map<String, Object> asObjectMap(Object rawValue) {
        if (rawValue instanceof Map<?, ?> map) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            return normalized;
        }
        return Collections.emptyMap();
    }

    private long toLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private String formatPercent(Object value) {
        if (value == null) {
            return "N/A";
        }
        if (value instanceof Number number) {
            return String.format("%.1f%%", number.doubleValue());
        }
        return String.valueOf(value);
    }

    private List<Map<String, String>> buildGeneralMetadataEntries(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, String>> entries = new ArrayList<>();
        Map<String, Object> fileInfo = asObjectMap(metadata.get("file_info"));
        Map<String, Object> dimensions = asObjectMap(metadata.get("dimensions"));
        Map<String, Object> qualityMetrics = asObjectMap(metadata.get("quality_metrics"));

        addMetadataEntry(entries, "file_name", fileInfo.get("file_name"));
        addMetadataEntry(entries, "file_type", fileInfo.get("file_type"));
        addMetadataEntry(entries, "file_size_bytes", fileInfo.get("file_size_bytes"));
        addMetadataEntry(entries, "uploaded_by", fileInfo.get("uploaded_by"));
        addMetadataEntry(entries, "uploaded_at", fileInfo.get("uploaded_at"));

        addMetadataEntry(entries, "row_count", dimensions.get("row_count"));
        addMetadataEntry(entries, "column_count", dimensions.get("column_count"));

        addMetadataEntry(entries, "completeness", qualityMetrics.get("completeness"));
        addMetadataEntry(entries, "data_type_distribution", qualityMetrics.get("data_type_distribution"));
        addMetadataEntry(entries, "extraction_version", qualityMetrics.get("extraction_version"));

        addMetadataEntry(entries, "metadata_extracted_at", metadata.get("metadata_extracted_at"));

        if (entries.isEmpty()) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                if ("columns".equals(entry.getKey())) {
                    continue;
                }
                addMetadataEntry(entries, entry.getKey(), entry.getValue());
            }
        }

        return entries;
    }

    private List<Map<String, Object>> buildTableDetails(List<DatasetTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> details = new ArrayList<>();
        for (DatasetTable table : tables) {
            Map<String, Object> parsedMetadata = parseTableMetadata(table);
            List<Map<String, Object>> metadataColumns = extractMetadataColumns(parsedMetadata);
            Map<String, Object> dimensions = asObjectMap(parsedMetadata.get("dimensions"));
            Map<String, Object> qualityMetrics = asObjectMap(parsedMetadata.get("quality_metrics"));

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("tableId", table.getTableId());
            detail.put("tableName", table.getTableName());
            detail.put("fileType", table.getFileType() != null ? table.getFileType().name() : "N/A");
            detail.put("status", table.getMetadataExtractionStatus() != null ? table.getMetadataExtractionStatus().name() : "PENDING");
            detail.put("extractedAt", table.getMetadataExtractedAt());
            detail.put("rowCount", toLong(dimensions.get("row_count"), 0L));
            detail.put("columnCount", toLong(dimensions.get("column_count"), metadataColumns.size()));
            detail.put("completeness", formatPercent(qualityMetrics.get("completeness")));
            detail.put("metadataColumns", metadataColumns);
            details.add(detail);
        }

        return details;
    }

    private List<Map<String, Object>> buildTablePreviews(List<DatasetTable> tables) {
        if (tables == null || tables.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> previews = new ArrayList<>();
        for (DatasetTable table : tables) {
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("tableId", table.getTableId());
            preview.put("tableName", table.getTableName());
            preview.put("fileType", table.getFileType() != null ? table.getFileType().name() : "N/A");
            preview.put("status", table.getMetadataExtractionStatus() != null ? table.getMetadataExtractionStatus().name() : "PENDING");

            List<Map<String, String>> rows = Collections.emptyList();
            try {
                rows = queryExecutionService.previewRows(table, 10);
            } catch (Exception ignored) {
                rows = Collections.emptyList();
            }

            preview.put("rows", rows);
            previews.add(preview);
        }

        return previews;
    }

    private void addMetadataEntry(List<Map<String, String>> entries, String key, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value);
        if (text.isBlank()) {
            return;
        }

        Map<String, String> item = new LinkedHashMap<>();
        item.put("key", key);
        item.put("value", text);
        entries.add(item);
    }

    private String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "overview";
        }
        String candidate = tab.trim().toLowerCase();
        if ("schema".equals(candidate)) {
            return "overview";
        }
        return switch (candidate) {
            case "overview", "preview", "permissions", "discussion", "statistics" -> candidate;
            default -> "overview";
        };
    }

    private String resolveReadableFileSize(String filePathRaw) {
        if (filePathRaw == null || filePathRaw.isBlank()) {
            return "N/A";
        }

        try {
            Path filePath = Paths.get(filePathRaw);
            if (!Files.exists(filePath)) {
                return "N/A";
            }
            return humanReadableSize(Files.size(filePath));
        } catch (Exception ex) {
            return "N/A";
        }
    }

    private String humanReadableSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.1f MB", mb);
        }
        return String.format("%.1f GB", mb / 1024.0);
    }

    private void deleteDatasetFiles(Integer datasetId) {
        try {
            Path datasetDirectory = Paths.get(uploadDir, String.valueOf(datasetId));
            if (!Files.exists(datasetDirectory)) {
                return;
            }

            List<Path> paths = Files.walk(datasetDirectory)
                    .sorted((left, right) -> right.compareTo(left))
                    .toList();

            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {
            // Best effort cleanup; DB deletion already completed.
        }
    }

    private AccessLevel parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return AccessLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private FileType parseFileType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("XLS".equals(normalized)) {
            normalized = "XLSX";
        }
        try {
            return FileType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private LocalDateTime parseStartDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date).atTime(LocalTime.MAX);
    }

    private void createAuditLog(User user, Dataset dataset, String action, String details) {
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setDataset(dataset);
        log.setAction(action);
        log.setDetails(details);
        auditLogRepository.save(log);
    }

    private String csvCell(String value) {
        return "\"" + (value == null ? "" : value.replace("\"", "\"\"")) + "\"";
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String safeValue(String value) {
        return value == null ? "" : value;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String defaultUserDisplayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail();
    }

    private String ownerSearchLabel(User user) {
        return defaultUserDisplayName(user) + " (@" + user.getUsername() + ")";
    }

    private String joinRow(List<String> values, char delimiter) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(delimiter);
            }
            out.append(escapeDelimitedValue(values.get(i), delimiter));
        }
        return out.toString();
    }

    private String escapeDelimitedValue(String value, char delimiter) {
        String safe = value == null ? "" : value;
        boolean mustQuote = safe.indexOf(delimiter) >= 0 || safe.contains("\n") || safe.contains("\"");
        if (!mustQuote) {
            return safe;
        }
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
