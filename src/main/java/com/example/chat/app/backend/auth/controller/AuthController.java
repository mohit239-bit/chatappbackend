package com.example.chat.app.backend.auth.controller;

import com.example.chat.app.backend.auth.config.AuthProperties;
import com.example.chat.app.backend.auth.dto.AuthResponse;
import com.example.chat.app.backend.auth.dto.GoogleLoginRequest;
import com.example.chat.app.backend.auth.dto.LoginRequest;
import com.example.chat.app.backend.auth.dto.RegisterRequest;
import com.example.chat.app.backend.auth.dto.UserResponse;
import com.example.chat.app.backend.auth.dto.WebSocketTokenResponse;
import com.example.chat.app.backend.auth.security.ChatUserPrincipal;
import com.example.chat.app.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties properties;

    public AuthController(AuthService authService, AuthProperties properties) {
        this.authService = authService;
        this.properties = properties;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        return authenticatedResponse(authService.register(request), response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        return authenticatedResponse(authService.login(request), response, HttpStatus.OK);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request,
                                                        HttpServletResponse response) {
        return authenticatedResponse(authService.loginWithGoogle(request.credential()), response, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(properties.getCookieName(), "")
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
                .toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse currentUser(@AuthenticationPrincipal ChatUserPrincipal principal) {
        return UserResponse.from(authService.getCurrentUser(principal.id()));
    }

    @GetMapping("/websocket-token")
    public WebSocketTokenResponse webSocketToken(@AuthenticationPrincipal ChatUserPrincipal principal) {
        return new WebSocketTokenResponse(authService.createWebSocketToken(principal.id()));
    }

    private ResponseEntity<AuthResponse> authenticatedResponse(AuthService.AuthResult result,
                                                                HttpServletResponse response,
                                                                HttpStatus status) {
        ResponseCookie cookie = ResponseCookie.from(properties.getCookieName(), result.token())
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite(properties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ofMinutes(properties.getJwtExpirationMinutes()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.status(status).body(new AuthResponse(UserResponse.from(result.user())));
    }
}
