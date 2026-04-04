package com.onlinedatatepo.data_repository.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.TableVersion;
import com.onlinedatatepo.data_repository.entity.Version;
import com.onlinedatatepo.data_repository.repository.TableVersionRepository;
import com.onlinedatatepo.data_repository.repository.VersionRepository;

@Service
public class DatasetVersionService {

    private final VersionRepository versionRepository;
    private final TableVersionRepository tableVersionRepository;

    public DatasetVersionService(VersionRepository versionRepository,
                                 TableVersionRepository tableVersionRepository) {
        this.versionRepository = versionRepository;
        this.tableVersionRepository = tableVersionRepository;
    }

    @Transactional
    public Version createVersion(Dataset dataset, List<DatasetTable> tables, String changeSummary) {
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset is required");
        }

        long versionCount = versionRepository.countByDataset_DatasetId(dataset.getDatasetId());

        Version version = new Version();
        version.setDataset(dataset);
        version.setVersionNumber((int) versionCount + 1);
        version.setChangeSummary(changeSummary);
        Version savedVersion = versionRepository.save(version);

        List<DatasetTable> safeTables = tables == null ? Collections.emptyList() : tables;
        for (DatasetTable table : safeTables) {
            if (table == null || table.getTableId() == null) {
                continue;
            }
            TableVersion tableVersion = new TableVersion();
            tableVersion.setTable(table);
            tableVersion.setVersion(savedVersion);
            tableVersion.setTableVersionNo((int) tableVersionRepository.countByTable_TableId(table.getTableId()) + 1);
            tableVersionRepository.save(tableVersion);
        }

        return savedVersion;
    }
}
