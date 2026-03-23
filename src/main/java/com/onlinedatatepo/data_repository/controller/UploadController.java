package com.onlinedatatepo.data_repository.controller;

import com.onlinedatatepo.data_repository.entity.AccessLevel;
import com.onlinedatatepo.data_repository.entity.Dataset;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.service.AuthService;
import com.onlinedatatepo.data_repository.service.DatasetService;
import com.onlinedatatepo.data_repository.service.FileUploadService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        return "upload";
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file,
                             @RequestParam(value = "datasetName", required = false, defaultValue = "Untitled Dataset") String datasetName,
                             @RequestParam(value = "description", required = false, defaultValue = "") String description,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = authService.findByEmail(authentication.getName());

            // Create a new dataset for this upload
            Dataset dataset = datasetService.createDataset(
                    datasetName, description, null, AccessLevel.PRIVATE, user);

            // Upload file to the dataset
            fileUploadService.uploadFileForNewDataset(file, dataset);

            redirectAttributes.addFlashAttribute("success",
                    "File '" + file.getOriginalFilename() + "' uploaded successfully!");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + e.getMessage());
            return "redirect:/upload";
        }
    }
}
