package com.ogeedeveloper.backend.controller;

import com.ogeedeveloper.backend.dto.LoginRequestDto;
import com.ogeedeveloper.backend.dto.LoginResponseDto;
import com.ogeedeveloper.backend.dto.RegistrationRequestDto;
import com.ogeedeveloper.backend.dto.RegistrationResponseDto;
import com.ogeedeveloper.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
//    Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid LoginRequestDto loginRequestDto) {
        log.info("Login attempt for user: {}", loginRequestDto.getEmail());
        LoginResponseDto user = userService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponseDto> register(@Valid RegistrationRequestDto registrationRequestDto) {
        log.info("Registration attempt for user: {}", registrationRequestDto.getEmail());
        RegistrationResponseDto user = userService.register(registrationRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
