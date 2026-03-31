package com.onlinedatatepo.data_repository.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
                                 org.springframework.security.core.Authentication authentication,
                                 Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalDatasets", datasetRepository.count());
        model.addAttribute("totalViews", auditLogRepository.countByActionIgnoreCase("DATASET_VIEWED"));
        model.addAttribute("totalDownloads", auditLogRepository.countByActionIgnoreCase("DATASET_DOWNLOADED"));

        List<DatasetRepository.DatasetGrowthProjection> growthTimeline = datasetRepository.datasetGrowthTimeline();
        model.addAttribute("growthTimeline", growthTimeline);

        Page<AuditLogRepository.DatasetLeaderboardProjection> topViewed =
                auditLogRepository.topDatasetsByAction("DATASET_VIEWED", PageRequest.of(0, 10));
        Page<AuditLogRepository.DatasetLeaderboardProjection> topDownloaded =
                auditLogRepository.topDatasetsByAction("DATASET_DOWNLOADED", PageRequest.of(0, 10));
        Page<AuditLogRepository.UserActivityProjection> topUsers =
                auditLogRepository.topActiveUsers(PageRequest.of(0, 10));

        model.addAttribute("topViewedDatasets", topViewed.getContent());
        model.addAttribute("topDownloadedDatasets", topDownloaded.getContent());
        model.addAttribute("topActiveUsers", topUsers.getContent());

        Page<AuditLog> latestActivity = auditLogRepository.searchLogs(
                null,
                null,
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now(),
                null,
                PageRequest.of(Math.max(0, page), 50, Sort.by(Sort.Direction.DESC, "timestamp"))
        );
        model.addAttribute("activityRows", latestActivity.getContent());
        model.addAttribute("activityCurrentPage", latestActivity.getNumber());
        model.addAttribute("activityTotalPages", latestActivity.getTotalPages());

        return "admin-dashboard";
    }
}