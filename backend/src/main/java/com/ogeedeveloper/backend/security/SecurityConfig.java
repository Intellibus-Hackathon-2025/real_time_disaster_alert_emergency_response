package com.ogeedeveloper.backend.security;

import com.ogeedeveloper.backend.config.OAuth2LoginSuccessHandler;
import com.ogeedeveloper.backend.filter.JwtAuthenticationFilter;
import com.ogeedeveloper.backend.model.User;
import com.ogeedeveloper.backend.model.UserRole;
import com.ogeedeveloper.backend.repository.RoleRepository;
import com.ogeedeveloper.backend.repository.UserRepository;
import com.ogeedeveloper.backend.security.jwt.AuthEntryPointJwt;
import com.ogeedeveloper.backend.security.jwt.AuthTokenFilter;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Role;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true,
        securedEnabled = true,
        jsr250Enabled = true)
public class SecurityConfig {
    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Autowired
    @Lazy
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    @Lazy
    private PasswordEncoder passwordEncoder;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf ->
                        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .ignoringRequestMatchers("/api/auth/public/**")
                )
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/csrf-token").permitAll()
                        .requestMatchers("/api/auth/public/**").permitAll()
                        .requestMatchers("/api/v1/users/login").permitAll()
                        .requestMatchers("/api/v1/users/register").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .formLogin(withDefaults())
                .httpBasic(withDefaults());
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

//    @Bean
//    public CommandLineRunner initData() {
//        return args -> {
//            Role userRole = roleRepository.findByName(UserRole.ROLE_USER)
//                    .orElseGet(() -> {
//                        Role newRole = new Role();
//                        newRole.setName(UserRole.ROLE_USER);
//                        return roleRepository.save(newRole);
//                    });
//
//            Role adminRole = roleRepository.findByName(UserRole.ROLE_ADMIN)
//                    .orElseGet(() -> {
//                        Role newRole = new Role();
//                        newRole.setName(UserRole.ROLE_ADMIN);
//                        return roleRepository.save(newRole);
//                    });
//
//            if (!userRepository.existsByUsername("user1")) {
//                User user1 = new User();
//                user1.setFirstName("user1");
//                user1.setLastName("user1");
//                user1.setEmail("user1@example.com");
//                user1.setUsername("user1");
//                user1.setPassword(passwordEncoder.encode("password1"));
//                user1.setAccountNonLocked(true);
//                user1.setAccountNonExpired(true);
//                user1.setCredentialsNonExpired(true);
//                user1.setEnabled(true);
//                Set<Role> roles = new HashSet<>();
//                roles.add(userRole);
//                user1.setRoles(roles);
//                userRepository.save(user1);
//            }
//
//            if (!userRepository.existsByUsername("admin")) {
//                User admin = new User();
//                admin.setFirstName("admin");
//                admin.setLastName("admin");
//                admin.setEmail("admin@example.com");
//                admin.setUsername("admin");
//                admin.setPassword(passwordEncoder.encode("adminPass"));
//                admin.setAccountNonLocked(true);
//                admin.setAccountNonExpired(true);
//                admin.setCredentialsNonExpired(true);
//                admin.setEnabled(true);
//                Set<Role> roles = new HashSet<>();
//                roles.add(adminRole);
//                admin.setRoles(roles);
//                userRepository.save(admin);
//            }
//        };
//    }
}