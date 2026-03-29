package com.onlinedatatepo.data_repository.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.onlinedatatepo.data_repository.entity.AuditLog;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.AuditLogRepository;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;

@Controller
public class DashboardController {

    private final DatasetService datasetService;
    private final AuthService authService;
    private final AuditLogRepository auditLogRepository;

    public DashboardController(DatasetService datasetService, AuthService authService,
                               AuditLogRepository auditLogRepository) {
        this.datasetService = datasetService;
        this.authService = authService;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "category", required = false) String category,
                            Authentication authentication,
                            Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        List<Dataset> trendingDatasets = datasetService.getTrendingDatasetsForDashboard(
                user.getUserId(),
                search,
                category,
                6
        );
        model.addAttribute("trendingDatasets", trendingDatasets);

        // Recent activity (latest audit logs)
        List<AuditLog> recentActivity = auditLogRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();
        model.addAttribute("recentActivity", recentActivity);

        // Stats
        model.addAttribute("totalDatasets", datasetService.countAllDatasets());
        model.addAttribute("selectedSearch", search == null ? "" : search);
        model.addAttribute("selectedCategory", category == null ? "" : category);

        return "dashboard";
    }

    @GetMapping("/my-datasets")
    public String myDatasets(Authentication authentication, Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        Page<Dataset> myDatasets = datasetService.getDatasetsByUser(
                user.getUserId(),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("myDatasets", myDatasets.getContent());
        return "my-datasets";
    }
}
