package com.onlinedatatepo.data_repository.controller;

import com.onlinedatatepo.data_repository.entity.AuditLog;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.AuditLogRepository;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

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
    public String dashboard(Authentication authentication, Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        // Trending datasets (latest public ones)
        List<Dataset> trendingDatasets = datasetService.getTrendingDatasets(4);
        model.addAttribute("trendingDatasets", trendingDatasets);

        // Recent activity (latest audit logs)
        List<AuditLog> recentActivity = auditLogRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();
        model.addAttribute("recentActivity", recentActivity);

        // Stats
        model.addAttribute("totalDatasets", datasetService.countAllDatasets());

        return "dashboard";
    }
}
