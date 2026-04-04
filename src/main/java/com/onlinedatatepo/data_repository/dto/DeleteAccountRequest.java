package com.onlinedatatepo.data_repository.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeleteAccountRequest {

    @NotBlank(message = "Password is required")
    private String password;

    private boolean confirmDelete;
}
