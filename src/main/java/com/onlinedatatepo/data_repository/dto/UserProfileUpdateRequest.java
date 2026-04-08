package com.onlinedatatepo.data_repository.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;
}
