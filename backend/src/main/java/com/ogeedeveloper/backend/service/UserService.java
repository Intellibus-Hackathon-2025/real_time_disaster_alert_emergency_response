package com.ogeedeveloper.backend.service;

import com.ogeedeveloper.backend.dto.LoginResponseDto;
import com.ogeedeveloper.backend.model.User;
import com.ogeedeveloper.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private static UserRepository userRepository;

//    Login
    public LoginResponseDto login(String email, String password) {
//        return userRepository.findUserByEmail(email)
//                .filter(user -> user.getPassword().equals(password)).orElseThrow();\
//        TODO: Implement login functionality
        return null;
    }
}
