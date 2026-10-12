package com.smartuniversity.scheduler.platform.security;

import com.smartuniversity.scheduler.TestSecrets;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class JwtConfigurationTest {
    private final JwtConfiguration configuration =new JwtConfiguration();
    private final JwtProperties properties =new JwtProperties(TestSecrets.generate(), "test-issuer", "test-audience", 900);

    private String token(String issuer, String audience, String role, Instant issued, Instant expiry) {
        var builder =JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(UUID.randomUUID().toString()).issuedAt(issued).claim("role", role);
        if (expiry !=null) builder.expiresAt(expiry);
        return configuration.jwtEncoder(properties).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), builder.build())).getTokenValue();
    }

    @Test
    void validatesExpectedIssuerAudienceExpiryAndRole() {
        Instant now =Instant.now();
        String signed =token("test-issuer", "test-audience", "STUDENT", now, now.plusSeconds(900));
        assertThat(configuration.jwtDecoder(properties).decode(signed).getClaimAsString("role")).isEqualTo("STUDENT");
    }

    @Test
    void rejectsExpiredMissingExpiryWrongIssuerWrongAudienceAndUnknownRoles() {
        Instant now =Instant.now();
        for (String signed : List.of(
                token("test-issuer", "test-audience", "STUDENT", now.minusSeconds(2000), now.minusSeconds(1100)),
                token("test-issuer", "test-audience", "STUDENT", now, null),
                token("wrong-issuer", "test-audience", "STUDENT", now, now.plusSeconds(900)),
                token("test-issuer", "wrong-audience", "STUDENT", now, now.plusSeconds(900)),
                token("test-issuer", "test-audience", "SUPERUSER", now, now.plusSeconds(900)),
                token("test-issuer", "test-audience", "STUDENT", now, now.plusSeconds(901)))) {
            assertThatThrownBy(() -> configuration.jwtDecoder(properties).decode(signed)).isInstanceOf(JwtException.class);
        }
    }

    @Test
    void rejectsTokensSignedWithAnotherKey() {
        Instant now =Instant.now();
        String signed =token("test-issuer", "test-audience", "ADMIN", now, now.plusSeconds(900));
        var anotherKey =new JwtProperties(TestSecrets.generate(), "test-issuer", "test-audience", 900);
        assertThatThrownBy(() -> configuration.jwtDecoder(anotherKey).decode(signed)).isInstanceOf(JwtException.class);
    }

    @Test
    void failsClosedForMissingInvalidOrShortSigningKeys() {
        for (String key : new String[]{"", "not base64!", java.util.Base64.getEncoder().encodeToString(new byte[16])}) {
            var invalid =new JwtProperties(key, "test-issuer", "test-audience", 900);
            assertThatThrownBy(() -> configuration.jwtEncoder(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
