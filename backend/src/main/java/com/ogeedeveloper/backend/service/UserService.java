package com.ogeedeveloper.backend.service;

import com.ogeedeveloper.backend.dto.LoginResponseDto;
import com.ogeedeveloper.backend.dto.RegistrationRequestDto;
import com.ogeedeveloper.backend.dto.RegistrationResponseDto;
import com.ogeedeveloper.backend.model.User;
import com.ogeedeveloper.backend.repository.UserRepository;
import com.ogeedeveloper.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

//    Login
    public LoginResponseDto login(String email, String password) {
        log.info("Login attempt for user: {}", email);
        User user = userRepository.findByUsernameOrEmail(email, email).orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(email, password));
        return new LoginResponseDto(token);
    }

    public RegistrationResponseDto register(RegistrationRequestDto registrationRequestDto) {
//        Mapping of RegistrationRequestDto to User
        User user = new User();
        user.setFirstName(registrationRequestDto.getFirstName());
        user.setLastName(registrationRequestDto.getLastName());
        user.setEmail(registrationRequestDto.getEmail());
        user.setUsername(registrationRequestDto.getUsername());
        user.setPassword(passwordEncoder.encode(registrationRequestDto.getPassword()));
        user.setPermanentAddress(registrationRequestDto.getPermanentAddress());

//        Save user to database
        userRepository.save(user);
        return new RegistrationResponseDto();
    }

//    create findByEmil method
    public User findByEmail(String email) {
        return userRepository.findUserByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}
