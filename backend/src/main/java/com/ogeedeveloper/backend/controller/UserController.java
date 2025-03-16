package com.ogeedeveloper.backend.controller;

import com.ogeedeveloper.backend.dto.LoginRequestDto;
import com.ogeedeveloper.backend.dto.LoginResponseDto;
import com.ogeedeveloper.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
//    Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid LoginRequestDto loginRequestDto) {
        LoginResponseDto user = userService.login(loginRequestDto.getEmail(), loginRequestDto.getPassword());
        return ResponseEntity.status(HttpStatus.OK).body(user);
    }
}
