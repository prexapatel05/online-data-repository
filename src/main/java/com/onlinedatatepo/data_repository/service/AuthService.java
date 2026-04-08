package com.onlinedatatepo.data_repository.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinedatatepo.data_repository.dto.RegisterRequest;
import com.onlinedatatepo.data_repository.entity.User;
import com.onlinedatatepo.data_repository.entity.UserRole;
import com.onlinedatatepo.data_repository.repository.DatasetAccessRepository;
import com.onlinedatatepo.data_repository.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DatasetAccessRepository datasetAccessRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    public AuthService(UserRepository userRepository,
                       DatasetAccessRepository datasetAccessRepository,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.userRepository = userRepository;
        this.datasetAccessRepository = datasetAccessRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setUsername(generateUsername(request.getEmail()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);

        User savedUser = userRepository.save(user);
        mailService.sendWelcomeEmail(savedUser);
        return savedUser;
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public User updateProfile(User user, String fullName) {
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        String normalizedName = fullName == null ? "" : fullName.trim();

        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }

        user.setFullName(normalizedName);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteAccount(User user, String rawPassword) {
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("Password is incorrect");
        }

        datasetAccessRepository.deleteByUser_UserId(user.getUserId());
        if (user.getAuthorizedDatasets() != null) {
            user.getAuthorizedDatasets().clear();
        }
        userRepository.delete(user);
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0];
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        int suffix = 1;
        while (userRepository.existsByUsername(base + suffix)) {
            suffix++;
        }
        return base + suffix;
    }
}
