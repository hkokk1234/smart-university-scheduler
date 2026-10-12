package com.smartuniversity.scheduler.identity.application;

import com.smartuniversity.scheduler.identity.api.AuthResponse;
import com.smartuniversity.scheduler.identity.domain.User;
import com.smartuniversity.scheduler.platform.security.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {
    private final JwtEncoder encoder;
    private final JwtProperties properties;

    public TokenService(JwtEncoder encoder, JwtProperties properties) {
        this.encoder =encoder;
        this.properties =properties;
    }

    public AuthResponse issue(User user) {
        Instant now =Instant.now();
        var claims =JwtClaimsSet.builder().issuer(properties.issuer()).subject(user.getId().toString())
                .audience(List.of(properties.audience())).issuedAt(now).expiresAt(now.plusSeconds(properties.ttlSeconds()))
                .id(UUID.randomUUID().toString()).claim("role", user.getRole().name()).build();
        String token =encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims))
                .getTokenValue();
        return new AuthResponse(token, "Bearer", properties.ttlSeconds(), UserDtoMapper.toDto(user));
    }
}
