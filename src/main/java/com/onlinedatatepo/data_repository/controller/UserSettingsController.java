package com.onlinedatatepo.data_repository.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.onlinedatatepo.data_repository.dto.DeleteAccountRequest;
import com.onlinedatatepo.data_repository.dto.UserProfileUpdateRequest;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
public class UserSettingsController {

    private final AuthService authService;

    public UserSettingsController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/settings")
    public String settingsPage(org.springframework.security.core.Authentication authentication, Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        UserProfileUpdateRequest profileRequest = new UserProfileUpdateRequest();
        profileRequest.setFullName(user.getFullName() == null ? "" : user.getFullName());
        profileRequest.setEmail(user.getEmail() == null ? "" : user.getEmail());

        model.addAttribute("profileRequest", profileRequest);
        model.addAttribute("deleteRequest", new DeleteAccountRequest());
        return "settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(@Valid @ModelAttribute("profileRequest") UserProfileUpdateRequest request,
                                BindingResult bindingResult,
                                org.springframework.security.core.Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("deleteRequest", new DeleteAccountRequest());

        if (bindingResult.hasErrors()) {
            return "settings";
        }

        try {
            authService.updateProfile(user, request.getFullName(), request.getEmail());
            redirectAttributes.addFlashAttribute("success", "Profile updated successfully.");
            return "redirect:/settings";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "settings";
        }
    }

    @PostMapping("/settings/delete-account")
    public String deleteAccount(@Valid @ModelAttribute("deleteRequest") DeleteAccountRequest request,
                                BindingResult bindingResult,
                                org.springframework.security.core.Authentication authentication,
                                RedirectAttributes redirectAttributes,
                                Model model,
                                HttpServletRequest httpServletRequest) {
        User user = authService.findByEmail(authentication.getName());
        model.addAttribute("user", user);

        UserProfileUpdateRequest profileRequest = new UserProfileUpdateRequest();
        profileRequest.setFullName(user.getFullName() == null ? "" : user.getFullName());
        profileRequest.setEmail(user.getEmail() == null ? "" : user.getEmail());
        model.addAttribute("profileRequest", profileRequest);

        if (bindingResult.hasErrors()) {
            return "settings";
        }
        if (!request.isConfirmDelete()) {
            model.addAttribute("deleteError", "Please confirm account deletion before continuing.");
            return "settings";
        }

        try {
            authService.deleteAccount(user, request.getPassword());
            httpServletRequest.getSession().invalidate();
            redirectAttributes.addFlashAttribute("success", "Account deleted successfully.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("deleteError", ex.getMessage());
            return "settings";
        }
    }
}
