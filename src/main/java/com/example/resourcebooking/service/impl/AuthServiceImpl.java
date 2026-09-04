package com.example.resourcebooking.service.impl;

import com.example.resourcebooking.dto.auth.LoginRequest;
import com.example.resourcebooking.dto.auth.LoginResponse;
import com.example.resourcebooking.dto.auth.RegisterRequest;
import com.example.resourcebooking.dto.user.UserResponse;
import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.Role;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ReservationConflictException;
import com.example.resourcebooking.mapper.UserMapper;
import com.example.resourcebooking.repository.UserRepository;
import com.example.resourcebooking.security.JwtService;
import com.example.resourcebooking.security.UserPrincipal;
import com.example.resourcebooking.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        log.info("Attempting to register new user with username: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration failed: Username '{}' already exists", request.getUsername());
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: Email '{}' already in use", request.getEmail());
            throw new ReservationConflictException("Email is already in use");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.USER;

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());

        return userMapper.toResponse(savedUser);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting authentication for username: {}", request.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        log.info("User '{}' successfully authenticated", principal.getUsername());

        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getJwtExpirationInMs() / 1000,
                principal.getUsername(),
                principal.getRole().name()
        );
    }
}
