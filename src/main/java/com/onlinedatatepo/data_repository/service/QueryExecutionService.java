package com.onlinedatatepo.data_repository.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.FileType;

@Service
public class QueryExecutionService {

    private static final Set<String> ALLOWED_OPERATORS = Set.of("=", "!=", ">", ">=", "<", "<=", "contains");

    public QueryResult executeSelectProjection(DatasetTable table,
                                               List<String> selectedColumns,
                                               List<QueryFilter> filters,
                                               int page,
                                               int size) {
        return runQuery(table, selectedColumns, filters, page, size, false, 0);
    }

    public QueryResult executeSelectProjectionForExport(DatasetTable table,
                                                        List<String> selectedColumns,
                                                        List<QueryFilter> filters,
                                                        int maxRows) {
        return runQuery(table, selectedColumns, filters, 0, maxRows, true, maxRows);
    }

    public List<Map<String, String>> previewRows(DatasetTable table, int limit) {
        QueryResult result = runQuery(table, Collections.emptyList(), Collections.emptyList(), 0, limit, true, limit);
        return result.rows();
    }

    private QueryResult runQuery(DatasetTable table,
                                 List<String> selectedColumns,
                                 List<QueryFilter> filters,
                                 int page,
                                 int size,
                                 boolean exportMode,
                                 int exportLimit) {
        if (table.getFileType() == FileType.XLSX) {
            throw new IllegalArgumentException("Query operations currently support CSV and TSV files.");
        }

        Path filePath = Path.of(table.getFilePath());
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("Dataset file was not found at " + table.getFilePath());
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                throw new IllegalArgumentException("Dataset file is empty.");
            }

            char delimiter = table.getFileType() == FileType.TSV ? '\t' : ',';
            List<String> availableColumns = parseDelimitedLine(headerLine, delimiter);
            if (availableColumns.isEmpty()) {
                throw new IllegalArgumentException("No readable columns were found in this dataset.");
            }

            List<String> normalizedSelectedColumns = normalizeSelectedColumns(availableColumns, selectedColumns);
            List<QueryFilter> normalizedFilters = normalizeFilters(availableColumns, filters);

            int rowStart = page * size;
            int rowEndExclusive = rowStart + size;

            int matchedRows = 0;
            boolean truncated = false;
            List<Map<String, String>> pageRows = new ArrayList<>();

            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                Map<String, String> row = parseRow(availableColumns, dataLine, delimiter);
                if (!matchesAllFilters(row, normalizedFilters)) {
                    continue;
                }

                if (exportMode) {
                    if (exportLimit > 0 && matchedRows >= exportLimit) {
                        truncated = true;
                        break;
                    }
                    pageRows.add(projectColumns(row, normalizedSelectedColumns));
                    matchedRows++;
                    continue;
                }

                if (matchedRows >= rowStart && matchedRows < rowEndExclusive) {
                    pageRows.add(projectColumns(row, normalizedSelectedColumns));
                }
                matchedRows++;
            }

            String sqlPreview = buildSqlPreview(table.getTableName(), normalizedSelectedColumns, normalizedFilters);
            return new QueryResult(normalizedSelectedColumns, pageRows, matchedRows, truncated, sqlPreview);
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read dataset file for query execution.", e);
        }
    }

    private List<String> normalizeSelectedColumns(List<String> availableColumns, List<String> selectedColumns) {
        if (selectedColumns == null || selectedColumns.isEmpty()) {
            return availableColumns;
        }

        Set<String> available = new LinkedHashSet<>(availableColumns);
        List<String> normalized = selectedColumns.stream()
                .filter(col -> col != null && !col.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());

        if (normalized.isEmpty()) {
            return availableColumns;
        }

        List<String> missing = normalized.stream().filter(col -> !available.contains(col)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Invalid selected column(s): " + String.join(", ", missing));
        }

        return normalized;
    }

    private List<QueryFilter> normalizeFilters(List<String> availableColumns, List<QueryFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> available = new LinkedHashSet<>(availableColumns);
        List<QueryFilter> normalized = new ArrayList<>();

        for (QueryFilter filter : filters) {
            if (filter == null) {
                continue;
            }

            if (filter.column() == null || filter.column().isBlank() || filter.operator() == null || filter.operator().isBlank()) {
                continue;
            }

            String column = filter.column().trim();
            String operator = filter.operator().trim().toLowerCase(Locale.ROOT);
            String value = filter.value() == null ? "" : filter.value().trim();

            if (!available.contains(column)) {
                throw new IllegalArgumentException("Invalid filter column: " + column);
            }

            if (!ALLOWED_OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("Invalid filter operator: " + operator);
            }

            normalized.add(new QueryFilter(column, operator, value));
        }

        return normalized;
    }

    private boolean matchesAllFilters(Map<String, String> row, List<QueryFilter> filters) {
        for (QueryFilter filter : filters) {
            String cell = row.getOrDefault(filter.column(), "");
            if (!matchesFilter(cell, filter.operator(), filter.value())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesFilter(String cellValue, String operator, String expectedValue) {
        String left = cellValue == null ? "" : cellValue;
        String right = expectedValue == null ? "" : expectedValue;

        return switch (operator) {
            case "=" -> left.equalsIgnoreCase(right);
            case "!=" -> !left.equalsIgnoreCase(right);
            case "contains" -> left.toLowerCase(Locale.ROOT).contains(right.toLowerCase(Locale.ROOT));
            case ">" -> compareValues(left, right) > 0;
            case ">=" -> compareValues(left, right) >= 0;
            case "<" -> compareValues(left, right) < 0;
            case "<=" -> compareValues(left, right) <= 0;
            default -> false;
        };
    }

    private int compareValues(String left, String right) {
        Double leftNum = parseDoubleOrNull(left);
        Double rightNum = parseDoubleOrNull(right);
        if (leftNum != null && rightNum != null) {
            return Double.compare(leftNum, rightNum);
        }
        return left.compareToIgnoreCase(right);
    }

    private Double parseDoubleOrNull(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, String> parseRow(List<String> columns, String line, char delimiter) {
        List<String> values = parseDelimitedLine(line, delimiter);
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            String value = i < values.size() ? values.get(i) : "";
            row.put(columns.get(i), value);
        }
        return row;
    }

    private Map<String, String> projectColumns(Map<String, String> row, List<String> selectedColumns) {
        Map<String, String> projected = new LinkedHashMap<>();
        for (String column : selectedColumns) {
            projected.put(column, row.getOrDefault(column, ""));
        }
        return projected;
    }

    private List<String> parseDelimitedLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        values.add(current.toString().trim());
        return values;
    }

    private String buildSqlPreview(String tableName, List<String> selectedColumns, List<QueryFilter> filters) {
        String selectClause = selectedColumns.stream().collect(Collectors.joining(", "));
        StringBuilder sql = new StringBuilder("SELECT ").append(selectClause)
                .append(" FROM ").append(tableName);

        if (!filters.isEmpty()) {
            sql.append(" WHERE ");
            String whereClause = filters.stream()
                    .map(filter -> {
                        if ("contains".equals(filter.operator())) {
                            return filter.column() + " ILIKE '%" + escapeSqlLiteral(filter.value()) + "%'";
                        }
                        return filter.column() + " " + filter.operator() + " '" + escapeSqlLiteral(filter.value()) + "'";
                    })
                    .collect(Collectors.joining(" AND "));
            sql.append(whereClause);
        }

        return sql.toString();
    }

    private String escapeSqlLiteral(String value) {
        return value == null ? "" : value.replace("'", "''");
    }

    public record QueryFilter(String column, String operator, String value) {
    }

    public record QueryResult(List<String> columns,
                              List<Map<String, String>> rows,
                              int totalMatchedRows,
                              boolean truncated,
                              String sqlPreview) {
    }
}
