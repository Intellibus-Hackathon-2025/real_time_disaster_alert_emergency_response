package com.ogeedeveloper.backend.dto;
import lombok.Data;

@Data
public class RegistrationRequestDto {
    private String firstName;
    private String lastName;
    private String password;
    private String username;
    private String email;
    private String permanentAddress;
}
