package com.example.chat.app.backend.auth.service;

import com.example.chat.app.backend.auth.config.AuthProperties;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

@Service
public class GoogleIdentityService {

    private final AuthProperties properties;

    public GoogleIdentityService(AuthProperties properties) {
        this.properties = properties;
    }

    public GoogleProfile verify(String credential) {
        if (properties.getGoogleClientId() == null || properties.getGoogleClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Google sign-in is not configured");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(properties.getGoogleClientId()))
                    .build();

            GoogleIdToken token = verifier.verify(credential);
            if (token == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Google credential is invalid or expired");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified()) || payload.getEmail() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Google account email is not verified");
            }

            Object providedName = payload.get("name");
            String name = providedName instanceof String ? (String) providedName : null;
            if (name == null || name.isBlank()) {
                name = payload.getEmail().split("@", 2)[0];
            }
            Object picture = payload.get("picture");
            return new GoogleProfile(payload.getEmail(), name, picture instanceof String ? (String) picture : null);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google credential could not be verified");
        }
    }

    public record GoogleProfile(String email, String name, String profilePicture) {
    }
}
