package com.onlinedatatepo.data_repository.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.onlinedatatepo.data_repository.entity.AuditLog;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.AuditLogRepository;
import com.onlinedatatepo.data_repository.repository.DatasetRepository;
import com.onlinedatatepo.data_repository.repository.UserRepository;
import com.onlinedatatepo.data_repository.service.AuthService;

@Controller
public class AdminController {

        private static final int EXPORT_MAX_ROWS = 5000;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final DatasetRepository datasetRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminController(AuthService authService,
                           UserRepository userRepository,
                           DatasetRepository datasetRepository,
                           AuditLogRepository auditLogRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.datasetRepository = datasetRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/admin")
    public String adminDashboard(@RequestParam(value = "page", defaultValue = "0") int page,
                                 @RequestParam(value = "period", defaultValue = "weekly") String period,
                                 @RequestParam(value = "userId", required = false) Integer userId,
                                 @RequestParam(value = "action", required = false) String action,
                                 @RequestParam(value = "startDate", required = false) String startDate,
                                 @RequestParam(value = "endDate", required = false) String endDate,
                                 org.springframework.security.core.Authentication authentication,
                                 Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalCreated", datasetRepository.count());
        model.addAttribute("totalViews", auditLogRepository.countByActionIgnoreCase("DATASET_VIEWED"));
        model.addAttribute("totalDownloads", auditLogRepository.countByActionIgnoreCase("DATASET_DOWNLOADED"));

        String selectedPeriod = "monthly".equalsIgnoreCase(period) ? "monthly" : "weekly";
        List<DatasetRepository.DatasetGrowthProjection> growthTimeline = "monthly".equals(selectedPeriod)
                ? datasetRepository.datasetGrowthTimelineMonthly()
                : datasetRepository.datasetGrowthTimelineWeekly();
        model.addAttribute("growthTimeline", growthTimeline);
        model.addAttribute("selectedPeriod", selectedPeriod);

        Page<AuditLogRepository.DatasetLeaderboardProjection> topViewed =
                auditLogRepository.topDatasetsByAction("DATASET_VIEWED", PageRequest.of(0, 10));
        Page<AuditLogRepository.DatasetLeaderboardProjection> topDownloaded =
                auditLogRepository.topDatasetsByAction("DATASET_DOWNLOADED", PageRequest.of(0, 10));
        Page<AuditLogRepository.UserActivityProjection> topUsers =
                auditLogRepository.topActiveUsers(PageRequest.of(0, 10));

        model.addAttribute("topViewedDatasets", topViewed.getContent());
        model.addAttribute("topDownloadedDatasets", topDownloaded.getContent());
        model.addAttribute("topActiveUsers", topUsers.getContent());

                LocalDateTime start = parseStartDate(startDate);
                LocalDateTime end = parseEndDate(endDate);
                Page<AuditLog> latestActivity = auditLogRepository.searchLogs(
                                userId,
                                normalizeText(action),
                                start,
                                end,
                                null,
                                PageRequest.of(Math.max(0, page), 50, Sort.by(Sort.Direction.DESC, "timestamp"))
                );
        model.addAttribute("activityRows", latestActivity.getContent());
        model.addAttribute("activityCurrentPage", latestActivity.getNumber());
        model.addAttribute("activityTotalPages", latestActivity.getTotalPages());
                model.addAttribute("allUsers", userRepository.findAll(Sort.by(Sort.Direction.ASC, "fullName")));
                model.addAttribute("selectedUserId", userId);
                model.addAttribute("selectedAction", safeValue(action));
                model.addAttribute("startDate", safeValue(startDate));
                model.addAttribute("endDate", safeValue(endDate));

        return "admin-dashboard";
    }

        @GetMapping("/admin/activity-export")
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
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=admin-activity-log.csv")
                                .contentType(MediaType.parseMediaType("text/csv"))
                                .body(body);
        }

        private LocalDateTime parseStartDate(String date) {
                if (date == null || date.isBlank()) {
                        return LocalDate.now().minusDays(30).atStartOfDay();
                }
                try {
                        return LocalDate.parse(date.trim()).atStartOfDay();
                } catch (Exception e) {
                        return LocalDate.now().minusDays(30).atStartOfDay();
                }
        }

        private LocalDateTime parseEndDate(String date) {
                if (date == null || date.isBlank()) {
                        return LocalDateTime.now();
                }
                try {
                        return LocalDate.parse(date.trim()).atTime(23, 59, 59);
                } catch (Exception e) {
                        return LocalDateTime.now();
                }
        }

        private String normalizeText(String value) {
                if (value == null) {
                        return null;
                }
                String normalized = value.trim();
                return normalized.isEmpty() ? null : normalized;
        }

        private String safeValue(String value) {
                return value == null ? "" : value;
        }

        private String csvCell(String value) {
                String sanitized = value == null ? "" : value.replace("\"", "\"\"");
                return "\"" + sanitized + "\"";
        }

        private String formatDateTime(LocalDateTime dateTime) {
                if (dateTime == null) {
                        return "";
                }
                return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        private String defaultUserDisplayName(User user) {
                if (user == null) {
                        return "System";
                }
                if (user.getFullName() != null && !user.getFullName().isBlank()) {
                        return user.getFullName();
                }
                return user.getUsername() != null ? user.getUsername() : "User";
        }
}