package com.onlinedatatepo.data_repository.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinedatatepo.data_repository.dto.DatasetStatsDTO;
import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetStatus;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.FileType;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.AuditLogRepository;
import com.onlinedatatepo.data_repository.repository.DatasetRepository;
import com.onlinedatatepo.data_repository.repository.DatasetTableRepository;
import com.onlinedatatepo.data_repository.repository.UserRepository;

@Service
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetTableRepository datasetTableRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public DatasetService(DatasetRepository datasetRepository,
                          DatasetTableRepository datasetTableRepository,
                          UserRepository userRepository,
                          AuditLogRepository auditLogRepository) {
        this.datasetRepository = datasetRepository;
        this.datasetTableRepository = datasetTableRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<Dataset> getPublicVerifiedDatasets() {
        return datasetRepository.findVerifiedDatasetsByAccessLevel(AccessLevel.PUBLIC);
    }

    public Page<Dataset> getDatasetsByUser(Integer userId, Pageable pageable) {
        return datasetRepository.findByUser_UserId(userId, pageable);
    }

    public Page<Dataset> searchOwnedDatasets(Integer userId,
                                             String search,
                                             String category,
                                             AccessLevel visibility,
                                             FileType fileType,
                                             Pageable pageable) {
        return datasetRepository.searchOwnedDatasets(
                userId,
                normalizeFilter(search),
                normalizeFilter(category),
                visibility != null ? visibility.name() : null,
                fileType != null ? fileType.name() : null,
                pageable
        );
    }

    public List<Dataset> getTrendingDatasets(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return datasetRepository.findByAccessLevel(AccessLevel.PUBLIC, pageable).getContent();
    }

    public List<Dataset> getTrendingDatasetsForDashboard(Integer currentUserId,
                                                         String search,
                                                         String category,
                                                         int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return datasetRepository.searchTrendingForDashboard(
                currentUserId,
                normalizeFilter(search),
                normalizeFilter(category),
                pageable
        ).getContent();
    }

    public Page<Dataset> getTrendingDatasetsForDashboardPage(Integer currentUserId,
                                                             String search,
                                                             String category,
                                                             Pageable pageable) {
        return datasetRepository.searchTrendingForDashboard(
                currentUserId,
                normalizeFilter(search),
                normalizeFilter(category),
                pageable
        );
    }

    public Page<Dataset> searchAccessibleDatasets(Integer currentUserId,
                                                  String search,
                                                  String category,
                                                  Integer ownerId,
                                                  AccessLevel visibility,
                                                  FileType fileType,
                                                  Pageable pageable) {
        return datasetRepository.searchAccessibleDatasets(
                currentUserId,
                normalizeFilter(search),
                normalizeFilter(category),
                ownerId,
                visibility,
                fileType,
                pageable
        );
    }

        public Page<Dataset> searchAccessibleDatasetsOrderByViews(Integer currentUserId,
                                      String search,
                                      String category,
                                      Integer ownerId,
                                      AccessLevel visibility,
                                      FileType fileType,
                                      Pageable pageable) {
        return datasetRepository.searchAccessibleDatasetsOrderByViews(
            currentUserId,
            normalizeFilter(search),
            normalizeFilter(category),
            ownerId,
            visibility != null ? visibility.name() : null,
            fileType != null ? fileType.name() : null,
            pageable
        );
        }

    public Page<Dataset> searchAccessibleDatasetsOrderByRating(Integer currentUserId,
                                      String search,
                                      String category,
                                      Integer ownerId,
                                      AccessLevel visibility,
                                      FileType fileType,
                                      Pageable pageable) {
        return datasetRepository.searchAccessibleDatasetsOrderByRating(
            currentUserId,
            normalizeFilter(search),
            normalizeFilter(category),
            ownerId,
            visibility != null ? visibility.name() : null,
            fileType != null ? fileType.name() : null,
            pageable
        );
    }

    public Page<Dataset> getSharedWithMeDatasets(Integer currentUserId, Pageable pageable) {
        return datasetRepository.findSharedWithMe(currentUserId, pageable);
    }

    public Page<Dataset> searchSharedWithMeDatasets(Integer currentUserId,
                                                    String search,
                                                    String category,
                                                    AccessLevel visibility,
                                                    FileType fileType,
                                                    Pageable pageable) {
        return datasetRepository.searchSharedWithMeDatasets(
                currentUserId,
                normalizeFilter(search),
                normalizeFilter(category),
                visibility != null ? visibility.name() : null,
                fileType != null ? fileType.name() : null,
                pageable
        );
    }

    public Dataset createDataset(String name, String description, String tag, AccessLevel accessLevel, User owner) {
        Dataset dataset = new Dataset();
        dataset.setName(name);
        dataset.setDescription(description);
        dataset.setTag(tag);
        dataset.setAccessLevel(accessLevel);
        dataset.setStatus(DatasetStatus.PENDING);
        dataset.setUser(owner);
        return datasetRepository.save(dataset);
    }

    public Dataset updateDatasetDetails(Dataset dataset, String name, String description, String tag) {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found");
        }

        if (name != null && !name.isBlank()) {
            dataset.setName(name.trim());
        }
        dataset.setDescription(description == null ? "" : description.trim());
        dataset.setTag(tag == null ? "" : tag.trim());
        return datasetRepository.save(dataset);
    }

    public long countAllDatasets() {
        return datasetRepository.count();
    }

    public long countByStatus(DatasetStatus status) {
        return datasetRepository.countByStatus(status);
    }

    public Optional<Dataset> findById(Integer datasetId) {
        return datasetRepository.findById(datasetId);
    }

    public List<DatasetTable> getTablesByDatasetId(Integer datasetId) {
        return datasetTableRepository.findByDataset_DatasetId(datasetId);
    }

    public Dataset updateAccessLevel(Dataset dataset, AccessLevel accessLevel) {
        dataset.setAccessLevel(accessLevel);
        return datasetRepository.save(dataset);
    }

    @Transactional
    public void deleteDataset(Dataset dataset) {
        if (dataset.getAuthorizedUsers() != null) {
            dataset.getAuthorizedUsers().clear();
        }
        datasetRepository.delete(dataset);
    }

    public PermissionUpdateResult updatePermissionsByEmails(Dataset dataset,
                                                            AccessLevel accessLevel,
                                                            List<String> authorizedEmails) {
        dataset.setAccessLevel(accessLevel);

        if (accessLevel != AccessLevel.AUTHORIZED) {
            dataset.setAuthorizedUsers(new ArrayList<>());
            Dataset saved = datasetRepository.save(dataset);
            return new PermissionUpdateResult(saved, List.of(), List.of());
        }

        Set<String> normalizedEmails = new LinkedHashSet<>();
        if (authorizedEmails != null) {
            for (String rawEmail : authorizedEmails) {
                if (rawEmail == null) {
                    continue;
                }
                String email = rawEmail.trim().toLowerCase();
                if (!email.isEmpty()) {
                    normalizedEmails.add(email);
                }
            }
        }

        normalizedEmails.remove(dataset.getUser().getEmail().trim().toLowerCase());

        List<User> resolvedUsers = new ArrayList<>();
        List<String> invalidEmails = new ArrayList<>();

        for (String email : normalizedEmails) {
            Optional<User> matchedUser = userRepository.findByEmail(email);
            if (matchedUser.isPresent()) {
                resolvedUsers.add(matchedUser.get());
            } else {
                invalidEmails.add(email);
            }
        }

        dataset.setAuthorizedUsers(resolvedUsers);
        Dataset savedDataset = datasetRepository.save(dataset);
        List<String> addedEmails = resolvedUsers.stream().map(User::getEmail).toList();
        return new PermissionUpdateResult(savedDataset, addedEmails, invalidEmails);
    }

    public boolean canAccessDataset(User user, Dataset dataset) {
        if (dataset.getUser().getUserId().equals(user.getUserId())) {
            return true;
        }
        if (dataset.getAccessLevel() == AccessLevel.PUBLIC) {
            return true;
        }

        if (dataset.getAccessLevel() == AccessLevel.AUTHORIZED && dataset.getAuthorizedUsers() != null) {
            return dataset.getAuthorizedUsers().stream()
                    .anyMatch(authorized -> authorized.getUserId().equals(user.getUserId()));
        }

        return false;
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public Map<Integer, DatasetStatsDTO> getDatasetStatsMap(List<Integer> datasetIds) {
        Map<Integer, DatasetStatsDTO> statsMap = new HashMap<>();
        if (datasetIds == null || datasetIds.isEmpty()) {
            return statsMap;
        }

        for (Integer datasetId : datasetIds) {
            statsMap.put(datasetId, new DatasetStatsDTO(datasetId, 0L, 0L, "N/A"));
        }

        List<String> trackedActions = List.of("DATASET_VIEWED", "DATASET_DOWNLOADED");
        List<AuditLogRepository.DatasetActionCountProjection> aggregatedCounts =
                auditLogRepository.countByDatasetIdsAndActions(datasetIds, trackedActions);

        for (AuditLogRepository.DatasetActionCountProjection row : aggregatedCounts) {
            DatasetStatsDTO current = statsMap.get(row.getDatasetId());
            if (current == null) {
                continue;
            }
            String action = row.getAction();
            Long totalValue = row.getTotal();
            long total = totalValue == null ? 0L : totalValue.longValue();
            if ("DATASET_VIEWED".equals(action)) {
                current.setViewCount(total);
            } else if ("DATASET_DOWNLOADED".equals(action)) {
                current.setDownloadCount(total);
            }
        }

        List<DatasetTableRepository.DatasetFileTypeProjection> fileTypes =
                datasetTableRepository.findFileTypesByDatasetIds(datasetIds);
        for (DatasetTableRepository.DatasetFileTypeProjection row : fileTypes) {
            DatasetStatsDTO current = statsMap.get(row.getDatasetId());
            if (current == null || (current.getFileType() != null && !"N/A".equals(current.getFileType()))) {
                continue;
            }
            if (row.getFileType() != null && !row.getFileType().isBlank()) {
                current.setFileType(row.getFileType());
            }
        }

        return statsMap;
    }

    public record PermissionUpdateResult(Dataset dataset,
                                         List<String> addedEmails,
                                         List<String> invalidEmails) {
    }
}

