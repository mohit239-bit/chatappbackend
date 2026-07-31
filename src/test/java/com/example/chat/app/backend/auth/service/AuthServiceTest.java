package com.example.chat.app.backend.auth.service;

import com.example.chat.app.backend.Respository.UserRepository;
import com.example.chat.app.backend.auth.dto.LoginRequest;
import com.example.chat.app.backend.auth.dto.RegisterRequest;
import com.example.chat.app.backend.auth.security.JwtService;
import com.example.chat.app.backend.entities.AuthProvider;
import com.example.chat.app.backend.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private GoogleIdentityService googleIdentityService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailAndHashesPassword() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.createToken(org.mockito.ArgumentMatchers.any(User.class))).thenReturn("access-token");

        AuthService.AuthResult result = authService.register(
                new RegisterRequest("Alice", " Alice@Example.com ", "password1"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("alice@example.com", saved.getEmail());
        assertEquals(AuthProvider.LOCAL, saved.getProvider());
        assertTrue(new BCryptPasswordEncoder().matches("password1", saved.getPassword()));
        assertEquals("access-token", result.token());
    }

    @Test
    void localLoginRejectsAnIncorrectPassword() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setProvider(AuthProvider.LOCAL);
        user.setPassword(new BCryptPasswordEncoder().encode("password1"));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginRequest("alice@example.com", "wrongpass1")));

        assertEquals(401, exception.getStatusCode().value());
    }
}
