package com.ogeedeveloper.backend.config;

import com.ogeedeveloper.backend.model.Role;
import com.ogeedeveloper.backend.model.User;
import com.ogeedeveloper.backend.model.UserRole;
import com.ogeedeveloper.backend.repository.RoleRepository;
import com.ogeedeveloper.backend.repository.UserRepository;
import com.ogeedeveloper.backend.security.jwt.JwtUtils;
import com.ogeedeveloper.backend.service.UserService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

        if ("github".equals(registrationId) || "google".equals(registrationId)) {
            DefaultOAuth2User principal = (DefaultOAuth2User) authentication.getPrincipal();
            Map<String, Object> attributes = principal.getAttributes();

            String email = attributes.getOrDefault("email", "").toString();
            String username;
            String idAttributeKey;

            if ("github".equals(registrationId)) {
                username = attributes.getOrDefault("login", "").toString();
                idAttributeKey = "id";
            } else {
                username = email.split("@")[0];
                idAttributeKey = "sub";
            }

            userRepository.findUserByEmail(email)
                    .ifPresentOrElse(user -> {
                        // User exists, update if needed
                        updateExistingUser(user, attributes, registrationId);
                        createAndSetAuthentication(user, attributes, idAttributeKey, oAuth2AuthenticationToken);
                    }, () -> {
                        // User does not exist, create a new user
                        User newUser = createNewUser(email, username, registrationId);
                        userRepository.save(newUser);
                        createAndSetAuthentication(newUser, attributes, idAttributeKey, oAuth2AuthenticationToken);
                    });

            // JWT TOKEN LOGIC
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
            String jwtToken = jwtUtils.generateTokenFromUsername(user);

            // Redirect to the frontend with the JWT token
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                    .queryParam("token", jwtToken)
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    private void updateExistingUser(User user, Map<String, Object> attributes, String registrationId) {
        // Update user information if necessary
        user.setProvider(registrationId);
        userRepository.save(user);
    }

    private User createNewUser(String email, String username, String registrationId) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setProvider(registrationId);

        // Set default role
        Role userRole = roleRepository.findByName(UserRole.ROLE_USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(UserRole.ROLE_USER);
                    return roleRepository.save(newRole);
                });
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        newUser.setRoles(roles);

        return newUser;
    }

    private void createAndSetAuthentication(User user, Map<String, Object> attributes, String idAttributeKey, OAuth2AuthenticationToken oAuth2AuthenticationToken) {
        DefaultOAuth2User oauthUser = new DefaultOAuth2User(
                user.getAuthorities(),
                attributes,
                idAttributeKey
        );

        Authentication securityAuth = new OAuth2AuthenticationToken(
                oauthUser,
                user.getAuthorities(),
                oAuth2AuthenticationToken.getAuthorizedClientRegistrationId()
        );

        SecurityContextHolder.getContext().setAuthentication(securityAuth);
    }
}