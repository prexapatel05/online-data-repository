package com.onlinedatatepo.data_repository.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.onlinedatatepo.data_repository.config.DatasetTagCatalog;
import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetFile;
import com.onlinedatatepo.data_repository.entity.DatasetFileCategory;
import com.onlinedatatepo.data_repository.entity.DatasetTable;
import com.onlinedatatepo.data_repository.entity.MetadataExtractionStatus;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.entity.Version;
import com.onlinedatatepo.data_repository.repository.VersionRepository;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import com.onlinedatatepo.data_repository.service.DatasetVersionService;
import com.onlinedatatepo.data_repository.service.FileUploadService;
import com.onlinedatatepo.data_repository.service.MetadataExtractionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class UploadController {

    private static final String EDIT_SUMMARY_SESSION_PREFIX = "edit.summary.";
    private static final String EDIT_REPLACED_FILE_SESSION_PREFIX = "edit.replaced-file.";

    private final FileUploadService fileUploadService;
    private final DatasetService datasetService;
    private final AuthService authService;
    private final MetadataExtractionService metadataExtractionService;
    private final DatasetVersionService datasetVersionService;
    private final VersionRepository versionRepository;

    public UploadController(FileUploadService fileUploadService,
                            DatasetService datasetService,
                            AuthService authService,
                            MetadataExtractionService metadataExtractionService,
                            DatasetVersionService datasetVersionService,
                            VersionRepository versionRepository) {
        this.fileUploadService = fileUploadService;
        this.datasetService = datasetService;
        this.authService = authService;
        this.metadataExtractionService = metadataExtractionService;
        this.datasetVersionService = datasetVersionService;
        this.versionRepository = versionRepository;
    }

    @GetMapping("/upload")
    public String uploadPage(Authentication authentication, Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("currentStep", 1);
        model.addAttribute("editingMode", false);
        model.addAttribute("datasetNameValue", "Untitled Dataset");
        model.addAttribute("datasetDescriptionValue", "");
        model.addAttribute("selectedTags", Collections.emptyList());
        model.addAttribute("versions", Collections.emptyList());
        model.addAttribute("availableTags", DatasetTagCatalog.TAGS);
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        return "upload";
    }

    @PostMapping("/upload/start")
    public String startUpload(@RequestParam(value = "files", required = false) MultipartFile[] files,
                              @RequestParam(value = "file", required = false) MultipartFile fallbackFile,
                              @RequestParam(value = "datasetName", required = false, defaultValue = "Untitled Dataset") String datasetName,
                              @RequestParam(value = "description", required = false, defaultValue = "") String description,
                              @RequestParam(value = "tags", required = false) List<String> tags,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = authService.findByEmail(authentication.getName());
            MultipartFile[] selectedFiles = normalizeDatasetFiles(files, fallbackFile);
            if (selectedFiles.length == 0) {
                throw new IllegalArgumentException("Please upload at least one dataset file.");
            }

            String resolvedName = resolveDatasetName(datasetName, selectedFiles[0]);
            String resolvedDescription = description == null ? "" : description.trim();
            String normalizedTags = DatasetTagCatalog.normalizeSelectedTags(tags);

            Dataset dataset = datasetService.createDataset(
                    resolvedName, resolvedDescription, normalizedTags, AccessLevel.PRIVATE, user);

            List<DatasetFile> uploadedFiles = fileUploadService.uploadFilesForNewDataset(selectedFiles, dataset);
            redirectAttributes.addFlashAttribute(
                    "success",
                    uploadedFiles.size() + " table file(s) uploaded. You can now attach additional documents."
            );
            return "redirect:/upload/" + dataset.getDatasetId() + "/documents";
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/upload";
        }
    }

    @GetMapping("/upload/{datasetId}/documents")
    public String additionalDocumentsPage(@PathVariable Integer datasetId,
                                          @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                          Authentication authentication,
                                          Model model,
                                          RedirectAttributes redirectAttributes,
                                          HttpServletRequest request) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("dataset", dataset);
        model.addAttribute("currentStep", 2);
        model.addAttribute("editingMode", editFlow);
        model.addAttribute("availableTags", DatasetTagCatalog.TAGS);
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("additionalDocuments", getAdditionalDocuments(datasetId));
        model.addAttribute(
                "authorizedEmails",
                dataset.getAuthorizedUsers() == null
                        ? Collections.emptyList()
                        : dataset.getAuthorizedUsers().stream()
                        .map(User::getEmail)
                        .filter(email -> email != null && !email.isBlank())
                        .collect(Collectors.toList())
        );
        model.addAttribute("basePath", request.getContextPath());
        return "upload";
    }

    @PostMapping("/upload/{datasetId}/documents")
    public String uploadAdditionalDocuments(@PathVariable Integer datasetId,
                                            @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                            @RequestParam("documents") MultipartFile[] documents,
                                            @RequestParam("documentCategory") DatasetFileCategory documentCategory,
                                            Authentication authentication,
                                            RedirectAttributes redirectAttributes) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        try {
            int uploadedCount = 0;
            for (MultipartFile document : documents) {
                if (document != null && !document.isEmpty()) {
                    fileUploadService.uploadAdditionalDocument(document, dataset, documentCategory);
                    uploadedCount++;
                }
            }

            if (uploadedCount == 0) {
                redirectAttributes.addFlashAttribute("error", "Please choose at least one document to upload.");
                return redirectToDocuments(datasetId, editFlow);
            }

            redirectAttributes.addFlashAttribute("success", uploadedCount + " additional document(s) uploaded.");
            return redirectToDocuments(datasetId, editFlow);
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("error", "Document upload failed: " + e.getMessage());
            return redirectToDocuments(datasetId, editFlow);
        }
    }

    @PostMapping("/upload/{datasetId}/documents/{fileId}/delete")
    public String deleteAdditionalDocument(@PathVariable Integer datasetId,
                                           @PathVariable Integer fileId,
                                           @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        DatasetFile file = fileUploadService.getFilesByDataset(datasetId).stream()
                .filter(item -> item.getFileId().equals(fileId))
                .findFirst()
                .orElse(null);

        if (file == null || file.getFileCategory() == DatasetFileCategory.DATASET) {
            redirectAttributes.addFlashAttribute("error", "Document not found.");
            return redirectToDocuments(datasetId, editFlow);
        }

        fileUploadService.deleteAdditionalDocument(file);
        redirectAttributes.addFlashAttribute("success", "Document removed successfully.");
        return redirectToDocuments(datasetId, editFlow);
    }

    @GetMapping("/upload/{datasetId}/metadata")
    public String metadataExtractorPage(@PathVariable Integer datasetId,
                                        @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                        Authentication authentication,
                                        Model model,
                                        RedirectAttributes redirectAttributes,
                                        HttpServletRequest request) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("dataset", dataset);
        model.addAttribute("currentStep", 3);
        model.addAttribute("editingMode", editFlow);
        model.addAttribute("availableTags", DatasetTagCatalog.TAGS);
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("additionalDocuments", getAdditionalDocuments(datasetId));
        List<DatasetTable> datasetTables = datasetService.getTablesByDatasetId(datasetId);
        model.addAttribute("datasetTables", datasetTables);
        model.addAttribute("hasPendingExtraction", datasetTables.stream()
                .anyMatch(table -> table.getMetadataExtractionStatus() == MetadataExtractionStatus.PENDING));
        model.addAttribute("basePath", request.getContextPath());
        return "upload";
    }

    @PostMapping("/upload/{datasetId}/metadata/reextract")
    public String reextractMetadataForEdit(@PathVariable Integer datasetId,
                                           @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                           Authentication authentication,
                                           RedirectAttributes redirectAttributes,
                                           HttpSession session) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        User user = authService.findByEmail(authentication.getName());
        List<DatasetTable> tables = datasetService.getTablesByDatasetId(datasetId);
        if (tables.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No dataset table found for metadata re-extraction.");
            return redirectToMetadata(datasetId, editFlow);
        }

        for (DatasetTable table : tables) {
            metadataExtractionService.reExtractMetadata(table, user);
        }

        String summary = resolveEditSummary(datasetId, session);
        datasetVersionService.createVersion(dataset, tables, summary);
        clearEditSession(datasetId, session);

        redirectAttributes.addFlashAttribute("success", "Metadata re-extraction started. Stay on this step to review updated metadata, then continue to permissions.");
        return redirectToMetadata(datasetId, editFlow);
    }

    @GetMapping("/upload/{datasetId}/permissions")
    public String permissionsPage(@PathVariable Integer datasetId,
                                  @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes,
                                  HttpServletRequest request) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("dataset", dataset);
        model.addAttribute("currentStep", 4);
        model.addAttribute("editingMode", editFlow);
        model.addAttribute("availableTags", DatasetTagCatalog.TAGS);
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("additionalDocuments", getAdditionalDocuments(datasetId));
        model.addAttribute(
                "authorizedEmails",
                dataset.getAuthorizedUsers() == null
                        ? Collections.emptyList()
                        : dataset.getAuthorizedUsers().stream()
                        .map(User::getEmail)
                        .filter(email -> email != null && !email.isBlank())
                        .collect(Collectors.toList())
        );
        model.addAttribute("basePath", request.getContextPath());
        return "upload";
    }

    @PostMapping("/upload/{datasetId}/permissions")
    public String savePermissions(@PathVariable Integer datasetId,
                                  @RequestParam(value = "editFlow", defaultValue = "false") boolean editFlow,
                                  @RequestParam("accessLevel") AccessLevel accessLevel,
                                  @RequestParam(value = "authorizedEmails", required = false) List<String> authorizedEmails,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        try {
            List<String> emails = authorizedEmails == null ? Collections.emptyList() : authorizedEmails;
            DatasetService.PermissionUpdateResult result = datasetService.updatePermissionsByEmails(
                    dataset,
                    accessLevel,
                    emails
            );

            if (!result.invalidEmails().isEmpty()) {
                redirectAttributes.addFlashAttribute(
                        "warning",
                        "These emails were not found and were skipped: " + String.join(", ", result.invalidEmails())
                );
            }

            redirectAttributes.addFlashAttribute("success",
                    "Dataset uploaded successfully with " + accessLevel + " access.");
            return editFlow ? "redirect:/datasets/" + datasetId + "?tab=overview" : "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Permission update failed: " + e.getMessage());
            return redirectToPermissions(datasetId, editFlow);
        }
    }

    @GetMapping("/upload/{datasetId}/edit")
    public String editDatasetPage(@PathVariable Integer datasetId,
                                  Authentication authentication,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        User user = authService.findByEmail(authentication.getName());
        List<Version> versions = versionRepository.findByDataset_DatasetIdOrderByVersionNumberDesc(datasetId);

        model.addAttribute("user", user);
        model.addAttribute("dataset", dataset);
        model.addAttribute("editingMode", true);
        model.addAttribute("datasetNameValue", dataset.getName());
        model.addAttribute("datasetDescriptionValue", dataset.getDescription());
        model.addAttribute("selectedTags", splitTags(dataset.getTag()));
        model.addAttribute("versions", versions);
        model.addAttribute("currentStep", 1);
        model.addAttribute("availableTags", DatasetTagCatalog.TAGS);
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        return "upload";
    }

    @PostMapping("/upload/{datasetId}/edit")
    public String editDataset(@PathVariable Integer datasetId,
                              @RequestParam(value = "datasetName", required = false) String datasetName,
                              @RequestParam(value = "description", required = false, defaultValue = "") String description,
                              @RequestParam(value = "tags", required = false) List<String> tags,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              @RequestParam(value = "changeSummary", required = false) String changeSummary,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        User user = authService.findByEmail(authentication.getName());
        List<DatasetTable> tables = datasetService.getTablesByDatasetId(datasetId);
        if (tables.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No dataset table found to update.");
            return "redirect:/upload/" + datasetId + "/edit";
        }

        try {
            String normalizedName = datasetName == null || datasetName.isBlank() ? dataset.getName() : datasetName.trim();
            String normalizedDescription = description == null ? "" : description.trim();
            String normalizedTags = DatasetTagCatalog.normalizeSelectedTags(tags);
            datasetService.updateDatasetDetails(dataset, normalizedName, normalizedDescription, normalizedTags);

            boolean replacedFile = file != null && !file.isEmpty();
            if (replacedFile) {
                fileUploadService.replacePrimaryDatasetFile(file, datasetId, user);
            }

            session.setAttribute(editSummarySessionKey(datasetId), changeSummary == null ? "" : changeSummary.trim());
            session.setAttribute(editFileReplacedSessionKey(datasetId), replacedFile);

            redirectAttributes.addFlashAttribute("success", "Step 1 saved. Continue to additional documents.");
            return redirectToDocuments(datasetId, true);
        } catch (IllegalArgumentException | IOException ex) {
            redirectAttributes.addFlashAttribute("error", "Dataset update failed: " + ex.getMessage());
            return "redirect:/upload/" + datasetId + "/edit";
        }
    }

    @PostMapping("/upload/{datasetId}/permissions/reset")
    public String resetPermissions(@PathVariable Integer datasetId,
                                   Authentication authentication,
                                   RedirectAttributes redirectAttributes) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        datasetService.updatePermissionsByEmails(dataset, AccessLevel.PRIVATE, Collections.emptyList());
        List<DatasetTable> tables = datasetService.getTablesByDatasetId(datasetId);
        datasetVersionService.createVersion(dataset, tables, "Reset permissions and reopened wizard flow");

        redirectAttributes.addFlashAttribute("success", "Permissions reset. Complete the wizard to publish access again.");
        return redirectToPermissions(datasetId, true);
    }

    private String redirectToDocuments(Integer datasetId, boolean editFlow) {
        return "redirect:/upload/" + datasetId + "/documents" + (editFlow ? "?editFlow=true" : "");
    }

    private String redirectToMetadata(Integer datasetId, boolean editFlow) {
        return "redirect:/upload/" + datasetId + "/metadata" + (editFlow ? "?editFlow=true" : "");
    }

    private String redirectToPermissions(Integer datasetId, boolean editFlow) {
        return "redirect:/upload/" + datasetId + "/permissions" + (editFlow ? "?editFlow=true" : "");
    }

    private String resolveEditSummary(Integer datasetId, HttpSession session) {
        Object explicitSummary = session.getAttribute(editSummarySessionKey(datasetId));
        String summary = explicitSummary == null ? "" : String.valueOf(explicitSummary).trim();
        if (!summary.isBlank()) {
            return summary;
        }

        Object replacedFileValue = session.getAttribute(editFileReplacedSessionKey(datasetId));
        boolean replacedFile = replacedFileValue instanceof Boolean b && b;
        return replacedFile
                ? "Updated dataset file and re-extracted metadata"
                : "Updated dataset metadata/documents and re-extracted metadata";
    }

    private void clearEditSession(Integer datasetId, HttpSession session) {
        session.removeAttribute(editSummarySessionKey(datasetId));
        session.removeAttribute(editFileReplacedSessionKey(datasetId));
    }

    private String editSummarySessionKey(Integer datasetId) {
        return EDIT_SUMMARY_SESSION_PREFIX + datasetId;
    }

    private String editFileReplacedSessionKey(Integer datasetId) {
        return EDIT_REPLACED_FILE_SESSION_PREFIX + datasetId;
    }

    private Dataset resolveOwnedDataset(Integer datasetId,
                                        Authentication authentication,
                                        RedirectAttributes redirectAttributes) {
        User user = authService.findByEmail(authentication.getName());
        Dataset dataset = datasetService.findById(datasetId).orElse(null);

        if (dataset == null) {
            redirectAttributes.addFlashAttribute("error", "Dataset not found.");
            return null;
        }

        if (!dataset.getUser().getUserId().equals(user.getUserId())) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to edit this dataset.");
            return null;
        }

        return dataset;
    }

    private String resolveDatasetName(String datasetName, MultipartFile file) {
        if (datasetName != null) {
            String trimmed = datasetName.trim();
            if (!trimmed.isEmpty() && !"Untitled Dataset".equalsIgnoreCase(trimmed)) {
                return trimmed;
            }
        }

        if (file != null && file.getOriginalFilename() != null) {
            String original = file.getOriginalFilename().trim();
            if (!original.isEmpty()) {
                int dotIndex = original.lastIndexOf('.');
                if (dotIndex > 0) {
                    return original.substring(0, dotIndex);
                }
                return original;
            }
        }

        return "Untitled Dataset";
    }

    private List<String> splitTags(String rawTag) {
        if (rawTag == null || rawTag.isBlank()) {
            return Collections.emptyList();
        }

        return java.util.Arrays.stream(rawTag.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private MultipartFile[] normalizeDatasetFiles(MultipartFile[] files, MultipartFile fallbackFile) {
        if (files != null && files.length > 0) {
            List<MultipartFile> validFiles = new ArrayList<>();
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    validFiles.add(file);
                }
            }
            if (!validFiles.isEmpty()) {
                return validFiles.toArray(MultipartFile[]::new);
            }
        }
        if (fallbackFile != null && !fallbackFile.isEmpty()) {
            return new MultipartFile[] { fallbackFile };
        }
        return new MultipartFile[0];
    }

    private List<DatasetFileCategory> getDocumentCategories() {
        List<DatasetFileCategory> categories = new ArrayList<>();
        categories.add(DatasetFileCategory.LICENSE);
        categories.add(DatasetFileCategory.DOCUMENTATION);
        categories.add(DatasetFileCategory.OTHER);
        return categories;
    }

    private List<DatasetFile> getAdditionalDocuments(Integer datasetId) {
        List<DatasetFile> documents = new ArrayList<>();
        for (DatasetFileCategory category : getDocumentCategories()) {
            documents.addAll(fileUploadService.getFilesByDatasetAndCategory(datasetId, category));
        }
        return documents;
    }
}
