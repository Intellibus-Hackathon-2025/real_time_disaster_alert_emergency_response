package com.ogeedeveloper.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {
    @Email(message = "Please enter a valid email")
    private String email;
    @NotBlank(message = "Please enter you password")
    private String password;
}
