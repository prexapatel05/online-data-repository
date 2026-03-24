package com.onlinedatatepo.data_repository.controller;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.DatasetFile;
import com.onlinedatatepo.data_repository.entity.DatasetFileCategory;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import com.onlinedatatepo.data_repository.service.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class UploadController {

    private final FileUploadService fileUploadService;
    private final DatasetService datasetService;
    private final AuthService authService;

    public UploadController(FileUploadService fileUploadService, DatasetService datasetService,
                            AuthService authService) {
        this.fileUploadService = fileUploadService;
        this.datasetService = datasetService;
        this.authService = authService;
    }

    @GetMapping("/upload")
    public String uploadPage(Authentication authentication, Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("currentStep", 1);
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        return "upload";
    }

    @PostMapping("/upload/start")
    public String startUpload(@RequestParam("file") MultipartFile file,
                              @RequestParam(value = "datasetName", required = false, defaultValue = "Untitled Dataset") String datasetName,
                              @RequestParam(value = "description", required = false, defaultValue = "") String description,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            User user = authService.findByEmail(authentication.getName());

            Dataset dataset = datasetService.createDataset(
                    datasetName, description, null, AccessLevel.PRIVATE, user);

            fileUploadService.uploadFileForNewDataset(file, dataset);
            redirectAttributes.addFlashAttribute("success",
                    "Dataset file uploaded. You can now attach additional documents.");
            return "redirect:/upload/" + dataset.getDatasetId() + "/documents";
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/upload";
        }
    }

    @GetMapping("/upload/{datasetId}/documents")
    public String additionalDocumentsPage(@PathVariable Integer datasetId,
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
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("additionalDocuments", getAdditionalDocuments(datasetId));
        model.addAttribute("basePath", request.getContextPath());
        return "upload";
    }

    @PostMapping("/upload/{datasetId}/documents")
    public String uploadAdditionalDocuments(@PathVariable Integer datasetId,
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
                return "redirect:/upload/" + datasetId + "/documents";
            }

            redirectAttributes.addFlashAttribute("success", uploadedCount + " additional document(s) uploaded.");
            return "redirect:/upload/" + datasetId + "/documents";
        } catch (IllegalArgumentException | IOException e) {
            redirectAttributes.addFlashAttribute("error", "Document upload failed: " + e.getMessage());
            return "redirect:/upload/" + datasetId + "/documents";
        }
    }

    @GetMapping("/upload/{datasetId}/metadata")
    public String metadataExtractorPage(@PathVariable Integer datasetId,
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
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("additionalDocuments", getAdditionalDocuments(datasetId));
        model.addAttribute("datasetTables", datasetService.getTablesByDatasetId(datasetId));
        model.addAttribute("basePath", request.getContextPath());
        return "upload";
    }

    @GetMapping("/upload/{datasetId}/permissions")
    public String permissionsPage(@PathVariable Integer datasetId,
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
        model.addAttribute("documentCategories", getDocumentCategories());
        model.addAttribute("accessLevels", AccessLevel.values());
        model.addAttribute("additionalDocuments", getAdditionalDocuments(datasetId));
        model.addAttribute("basePath", request.getContextPath());
        return "upload";
    }

    @PostMapping("/upload/{datasetId}/permissions")
    public String savePermissions(@PathVariable Integer datasetId,
                                  @RequestParam("accessLevel") AccessLevel accessLevel,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Dataset dataset = resolveOwnedDataset(datasetId, authentication, redirectAttributes);
        if (dataset == null) {
            return "redirect:/dashboard";
        }

        try {
            datasetService.updateAccessLevel(dataset, accessLevel);
            redirectAttributes.addFlashAttribute("success",
                    "Dataset uploaded successfully with " + accessLevel + " access.");
            return "redirect:/dashboard";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Permission update failed: " + e.getMessage());
            return "redirect:/upload/" + datasetId + "/permissions";
        }
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
