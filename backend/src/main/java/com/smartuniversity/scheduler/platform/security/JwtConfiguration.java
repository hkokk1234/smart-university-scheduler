package com.smartuniversity.scheduler.platform.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Configuration(proxyBeanMethods =false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {
    private SecretKeySpec key(JwtProperties properties) {
        byte[] decoded;
        try { decoded =Base64.getDecoder().decode(properties.secretBase64()); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("JWT_SECRET_BASE64 must be valid Base64"); }
        if (decoded.length < 32) throw new IllegalArgumentException("JWT signing key must contain at least 32 random bytes");
        return new SecretKeySpec(decoded, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(JwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(JwtProperties properties) {
        NimbusJwtDecoder decoder =NimbusJwtDecoder.withSecretKey(key(properties)).macAlgorithm(MacAlgorithm.HS256).build();
        var timestamps =new JwtTimestampValidator(Duration.ofSeconds(30));
        var issuer =new JwtIssuerValidator(properties.issuer());
        OAuth2TokenValidator<Jwt> claims =jwt -> {
            try {
                UUID.fromString(jwt.getSubject());
                String role =jwt.getClaimAsString("role");
                if (!("STUDENT".equals(role) || "ADMIN".equals(role))
                        || !jwt.getAudience().contains(properties.audience())
                        || jwt.getExpiresAt() ==null || jwt.getIssuedAt() ==null
                        || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                        || jwt.getIssuedAt().isAfter(Instant.now().plusSeconds(30))
                        || Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).getSeconds() > properties.ttlSeconds()) {
                    throw new IllegalArgumentException();
                }
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException error) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid token claims", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, issuer, claims));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
}
