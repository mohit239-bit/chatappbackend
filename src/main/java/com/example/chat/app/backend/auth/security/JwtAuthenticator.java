package com.example.chat.app.backend.auth.security;

import com.example.chat.app.backend.Respository.UserRepository;
import com.example.chat.app.backend.entities.User;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class JwtAuthenticator {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticator(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public Authentication authenticate(String token) {
        Claims claims = jwtService.parseClaims(token);
        if ("websocket".equals(claims.get("tokenType", String.class))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "WebSocket token cannot access HTTP APIs");
        }
        return authenticationFor(claims);
    }

    public Authentication authenticateWebSocket(String token) {
        Claims claims = jwtService.parseClaims(token);
        if (!"websocket".equals(claims.get("tokenType", String.class))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A WebSocket token is required");
        }
        return authenticationFor(claims);
    }

    private Authentication authenticationFor(Claims claims) {
        User user = userRepository.findById(claims.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is no longer available"));

        ChatUserPrincipal principal = new ChatUserPrincipal(user.getId(), user.getName(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.NO_AUTHORITIES);
    }
}
