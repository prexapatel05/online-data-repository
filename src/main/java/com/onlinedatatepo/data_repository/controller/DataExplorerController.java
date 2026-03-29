package com.onlinedatatepo.data_repository.controller;

import java.nio.charset.StandardCharsets;
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.AuditLog;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetColumn;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.FileType;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.AuditLogRepository;
import com.onlinedatatepo.data_repository.repository.DatasetColumnRepository;
import com.onlinedatatepo.data_repository.repository.UserRepository;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import com.onlinedatatepo.data_repository.service.QueryExecutionService;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class DataExplorerController {

    private static final int QUERY_PAGE_SIZE = 50;
    private static final int EXPORT_MAX_ROWS = 5000;

    private final DatasetService datasetService;
    private final AuthService authService;
    private final AuditLogRepository auditLogRepository;
    private final DatasetColumnRepository datasetColumnRepository;
    private final QueryExecutionService queryExecutionService;
    private final UserRepository userRepository;

    public DataExplorerController(DatasetService datasetService,
                                  AuthService authService,
                                  AuditLogRepository auditLogRepository,
                                  DatasetColumnRepository datasetColumnRepository,
                                  QueryExecutionService queryExecutionService,
                                  UserRepository userRepository) {
        this.datasetService = datasetService;
        this.authService = authService;
        this.auditLogRepository = auditLogRepository;
        this.datasetColumnRepository = datasetColumnRepository;
        this.queryExecutionService = queryExecutionService;
        this.userRepository = userRepository;
    }

    @GetMapping("/datasets")
    public String datasetsPage(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "ownerId", required = false) Integer ownerId,
                               @RequestParam(value = "visibility", required = false) String visibility,
                               @RequestParam(value = "format", required = false) String format,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "12") int size,
                               org.springframework.security.core.Authentication authentication,
                               Model model) {
        User currentUser = authService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);

        AccessLevel visibilityFilter = parseVisibility(visibility);
        FileType fileTypeFilter = parseFileType(format);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Dataset> datasets = datasetService.searchAccessibleDatasets(
                currentUser.getUserId(),
                search,
                category,
                ownerId,
                visibilityFilter,
                fileTypeFilter,
                pageable
        );

        model.addAttribute("datasets", datasets.getContent());
        model.addAttribute("totalPages", datasets.getTotalPages());
        model.addAttribute("currentPage", datasets.getNumber());
        model.addAttribute("totalItems", datasets.getTotalElements());

        model.addAttribute("search", safeValue(search));
        model.addAttribute("category", safeValue(category));
        model.addAttribute("ownerId", ownerId);
        model.addAttribute("visibility", visibilityFilter != null ? visibilityFilter.name() : "");
        model.addAttribute("format", fileTypeFilter != null ? fileTypeFilter.name() : "");
        model.addAttribute("owners", userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName")));

        return "datasets";
    }

    @GetMapping("/datasets/{datasetId}")
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
                previewRows = queryExecutionService.previewRows(primaryTable, 10);
            } catch (IllegalArgumentException ignored) {
                previewRows = Collections.emptyList();
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

        Map<String, Object> statistics = buildDatasetStatistics(columns, previewRows);

        model.addAttribute("dataset", dataset);
        model.addAttribute("tab", normalizeTab(tab));
        model.addAttribute("tables", tables);
        model.addAttribute("columns", columns);
        model.addAttribute("previewRows", previewRows);
        model.addAttribute("datasetActivity", datasetActivity);
        model.addAttribute("statistics", statistics);
        model.addAttribute("authorizedUsers", dataset.getAuthorizedUsers() == null ? Collections.emptyList() : dataset.getAuthorizedUsers());

        return "dataset-detail";
    }

    @GetMapping("/activity-log")
    public String activityLogPage(@RequestParam(value = "userId", required = false) Integer userId,
                                  @RequestParam(value = "action", required = false) String action,
                                  @RequestParam(value = "startDate", required = false) String startDate,
                                  @RequestParam(value = "endDate", required = false) String endDate,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "size", defaultValue = "20") int size,
                                  org.springframework.security.core.Authentication authentication,
                                  Model model) {
        User currentUser = authService.findByEmail(authentication.getName());
        model.addAttribute("user", currentUser);

        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = auditLogRepository.searchLogs(
                userId,
                normalizeText(action),
                start,
                end,
                null,
                pageable
        );

        model.addAttribute("logs", logs.getContent());
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("currentPage", logs.getNumber());
        model.addAttribute("totalActivities", auditLogRepository.count());
        model.addAttribute("downloadCount", auditLogRepository.countByActionIgnoreCase("DATASET_DOWNLOADED"));
        model.addAttribute("viewCount", auditLogRepository.countByActionIgnoreCase("DATASET_VIEWED"));
        model.addAttribute("editCount", auditLogRepository.countByActionIgnoreCase("DATASET_UPDATED"));
        model.addAttribute("allUsers", userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName")));
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedAction", safeValue(action));
        model.addAttribute("startDate", safeValue(startDate));
        model.addAttribute("endDate", safeValue(endDate));

        return "activity-log";
    }

    @GetMapping("/activity-log/export")
    public ResponseEntity<byte[]> exportActivityLog(@RequestParam(value = "userId", required = false) Integer userId,
                                                    @RequestParam(value = "action", required = false) String action,
                                                    @RequestParam(value = "startDate", required = false) String startDate,
                                                    @RequestParam(value = "endDate", required = false) String endDate) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        List<AuditLog> logs = auditLogRepository.searchLogs(
                userId,
                normalizeText(action),
                start,
                end,
                null,
                PageRequest.of(0, EXPORT_MAX_ROWS, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();

        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,User,Action,Dataset,Details\n");
        for (AuditLog log : logs) {
            csv.append(csvCell(formatDateTime(log.getTimestamp()))).append(',')
                    .append(csvCell(log.getUser() != null ? defaultUserDisplayName(log.getUser()) : "System")).append(',')
                    .append(csvCell(log.getAction())).append(',')
                    .append(csvCell(log.getDataset() != null ? log.getDataset().getName() : "N/A")).append(',')
                    .append(csvCell(log.getDetails()))
                    .append('\n');
        }

        byte[] body = csv.toString().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=activity-log.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(body);
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
                                                       List<Map<String, String>> previewRows) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalColumns", columns.size());
        stats.put("previewRows", previewRows.size());

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

    private String normalizeTab(String tab) {
        if (tab == null || tab.isBlank()) {
            return "overview";
        }
        String candidate = tab.trim().toLowerCase();
        return switch (candidate) {
            case "overview", "preview", "schema", "permissions", "activity", "statistics" -> candidate;
            default -> "overview";
        };
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
