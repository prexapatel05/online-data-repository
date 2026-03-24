package com.onlinedatatepo.data_repository.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetStatus;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.repository.DatasetRepository;
import com.onlinedatatepo.data_repository.repository.DatasetTableRepository;

@Service
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetTableRepository datasetTableRepository;

    public DatasetService(DatasetRepository datasetRepository, DatasetTableRepository datasetTableRepository) {
        this.datasetRepository = datasetRepository;
        this.datasetTableRepository = datasetTableRepository;
    }

    public List<Dataset> getPublicVerifiedDatasets() {
        return datasetRepository.findVerifiedDatasetsByAccessLevel(AccessLevel.PUBLIC);
    }

    public Page<Dataset> getDatasetsByUser(Integer userId, Pageable pageable) {
        return datasetRepository.findByUser_UserId(userId, pageable);
    }

    public List<Dataset> getTrendingDatasets(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return datasetRepository.findByAccessLevel(AccessLevel.PUBLIC, pageable).getContent();
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
}
