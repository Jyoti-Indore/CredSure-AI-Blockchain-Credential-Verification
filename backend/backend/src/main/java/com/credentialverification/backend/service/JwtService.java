
package com.credentialverification.backend.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.credentialverification.backend.entity.User;

@Service
public class JwtService {

    private static final long TOKEN_EXPIRATION_SECONDS = 3600;

    private final JwtEncoder jwtEncoder;

    public JwtService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String generateToken(User user) {

        Instant issuedAt = Instant.now();

        Instant expiresAt = issuedAt.plus(
                TOKEN_EXPIRATION_SECONDS,
                ChronoUnit.SECONDS
        );

        JwsHeader headers = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("credential-verification-system")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("authorities", List.of(
                        "ROLE_" + user.getRole().name()
                ))
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(headers, claims)
        ).getTokenValue();
    }

    public long getTokenExpirationSeconds() {
        return TOKEN_EXPIRATION_SECONDS;
    }
}
