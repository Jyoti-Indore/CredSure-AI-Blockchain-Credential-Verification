
package com.credentialverification.backend.dto;

public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String email;
    private String role;

    public AuthResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            String email,
            String role) {

        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.email = email;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
