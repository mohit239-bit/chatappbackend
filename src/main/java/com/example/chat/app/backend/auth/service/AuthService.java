package com.example.chat.app.backend.auth.service;

import com.example.chat.app.backend.Respository.UserRepository;
import com.example.chat.app.backend.auth.dto.LoginRequest;
import com.example.chat.app.backend.auth.dto.RegisterRequest;
import com.example.chat.app.backend.auth.security.JwtService;
import com.example.chat.app.backend.entities.AuthProvider;
import com.example.chat.app.backend.entities.User;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdentityService googleIdentityService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       GoogleIdentityService googleIdentityService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.googleIdentityService = googleIdentityService;
    }

    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String name = request.name().trim();
        if (name.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must be between 2 and 80 characters");
        }
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        validatePassword(request.password());

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setProvider(AuthProvider.LOCAL);
        user.setCreatedAt(Instant.now());
        return createResult(saveUser(user));
    }

    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(this::invalidCredentials);

        if (user.getProvider() != AuthProvider.LOCAL
                || user.getPassword() == null
                || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }
        return createResult(user);
    }

    public AuthResult loginWithGoogle(String credential) {
        GoogleIdentityService.GoogleProfile profile = googleIdentityService.verify(credential);
        String email = normalizeEmail(profile.email());
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setName(profile.name());
            newUser.setEmail(email);
            newUser.setProfilePicture(profile.profilePicture());
            newUser.setProvider(AuthProvider.GOOGLE);
            newUser.setCreatedAt(Instant.now());
            return saveUser(newUser);
        });

        if (user.getProvider() != AuthProvider.GOOGLE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This email is registered with email and password. Please sign in with your password.");
        }
        return createResult(user);
    }

    public User getCurrentUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is no longer available"));
    }

    public String createWebSocketToken(String id) {
        return jwtService.createWebSocketToken(getCurrentUser(id));
    }

    private AuthResult createResult(User user) {
        return new AuthResult(user, jwtService.createToken(user));
    }

    private User saveUser(User user) {
        try {
            return userRepository.save(user);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must include at least one letter and one number");
        }
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    public record AuthResult(User user, String token) {
    }
}
