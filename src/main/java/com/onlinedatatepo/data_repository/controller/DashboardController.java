package com.onlinedatatepo.data_repository.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.onlinedatatepo.data_repository.config.DatasetTagCatalog;
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
                            @RequestParam(value = "trendPage", defaultValue = "0") int trendPage,
                            Authentication authentication,
                            Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        Pageable trendPageable = PageRequest.of(Math.max(0, trendPage), 6, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Dataset> trendingDatasetsPage = datasetService.getTrendingDatasetsForDashboardPage(
                user.getUserId(),
                search,
                category,
                trendPageable
        );
        List<Dataset> trendingDatasets = trendingDatasetsPage.getContent();
        
        // Get stats for each trending dataset
        List<Integer> datasetIds = trendingDatasets.stream()
                .map(Dataset::getDatasetId)
                .toList();
        var datasetStatsMap = datasetService.getDatasetStatsMap(datasetIds);
        
        model.addAttribute("trendingDatasets", trendingDatasets);
        model.addAttribute("datasetStats", datasetStatsMap);
        model.addAttribute("trendCurrentPage", trendingDatasetsPage.getNumber());
        model.addAttribute("trendTotalPages", trendingDatasetsPage.getTotalPages());

        // Recent activity (latest audit logs)
        List<AuditLog> recentActivity = auditLogRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))
        ).getContent();
        model.addAttribute("recentActivity", recentActivity);

        // Stats
        model.addAttribute("totalDatasets", datasetService.countAllDatasets());
        model.addAttribute("selectedSearch", search == null ? "" : search);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("dashboardCategories", DatasetTagCatalog.TAGS);

        return "dashboard";
    }

    @GetMapping("/my-datasets")
    public String myDatasets(@RequestParam(value = "myPage", defaultValue = "0") int myPage,
                             @RequestParam(value = "sharedPage", defaultValue = "0") int sharedPage,
                             Authentication authentication,
                             Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        Page<Dataset> myDatasets = datasetService.getDatasetsByUser(
                user.getUserId(),
                PageRequest.of(Math.max(0, myPage), 12, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Page<Dataset> sharedWithMeDatasets = datasetService.getSharedWithMeDatasets(
            user.getUserId(),
            PageRequest.of(Math.max(0, sharedPage), 12, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<Integer> allDatasetIds = new java.util.ArrayList<>(myDatasets.getContent().stream()
                .map(Dataset::getDatasetId)
                .toList());
        allDatasetIds.addAll(sharedWithMeDatasets.getContent().stream()
                .map(Dataset::getDatasetId)
                .toList());
        
        var datasetStatsMap = datasetService.getDatasetStatsMap(allDatasetIds);

        model.addAttribute("myDatasets", myDatasets.getContent());
        model.addAttribute("sharedWithMeDatasets", sharedWithMeDatasets.getContent());
        model.addAttribute("datasetStats", datasetStatsMap);
        model.addAttribute("myCurrentPage", myDatasets.getNumber());
        model.addAttribute("myTotalPages", myDatasets.getTotalPages());
        model.addAttribute("sharedCurrentPage", sharedWithMeDatasets.getNumber());
        model.addAttribute("sharedTotalPages", sharedWithMeDatasets.getTotalPages());
        return "my-datasets";
    }
}
