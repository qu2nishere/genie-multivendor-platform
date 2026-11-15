package com.genie.common.security;

import com.genie.common.domain.UserRole;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

/**
 * Phát / kiểm access token JWT HS256 (NFR-05, plan AUTH A04.02) — dùng Nimbus có sẵn trong
 * {@code spring-boot-starter-oauth2-resource-server}, không thêm thư viện JWT.
 * <p>Claim: {@code sub} = userId, {@code role}, {@code sid} = phiên (family_id refresh token), {@code jti}, {@code iat}, {@code exp}.
 * Không đưa email / quyền vào token — quyền nạp lại mỗi request qua {@link AuthorityProvider}.
 */
@Component
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(SecurityProperties props, Clock clock) {
        byte[] secret = props.jwtSecret() == null ? new byte[0] : props.jwtSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("genie.security.jwt-secret (JWT_SECRET) phải dài ít nhất 32 byte (256 bit)");
        }
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamps.setClock(clock);
        nimbus.setJwtValidator(timestamps);
        this.decoder = nimbus;
        this.ttl = props.accessTokenTtl();
        this.clock = clock;
    }

    /** Phát access token mới cho phiên {@code sessionId}. */
    public IssuedAccessToken issue(long userId, UserRole role, long sessionId) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        Instant exp = now.plus(ttl);
        String jti = UUID.randomUUID().toString();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(exp)
                .id(jti)
                .claim("role", role.name())
                .claim("sid", sessionId)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, jti, now, exp, ttl.toSeconds());
    }

    /** Kiểm chữ ký + hạn; sai → {@link InvalidAccessTokenException}. */
    public AuthUser parse(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            Object sid = jwt.getClaim("sid");
            if (jwt.getSubject() == null || jwt.getId() == null || !(sid instanceof Number)) {
                throw new InvalidAccessTokenException("Thiếu claim bắt buộc");
            }
            return new AuthUser(Long.valueOf(jwt.getSubject()), UserRole.valueOf(jwt.getClaimAsString("role")),
                    ((Number) sid).longValue(), jwt.getId(), jwt.getIssuedAt(), jwt.getExpiresAt());
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            throw new InvalidAccessTokenException(e.getMessage());
        }
    }

    public Duration ttl() {
        return ttl;
    }

    /** Token đã phát. {@code expiresIn} (giây) là trường {@code expires_in} của {@code $AuthTokens}. */
    public record IssuedAccessToken(String token, String jti, Instant issuedAt, Instant expiresAt, long expiresIn) {
    }

    /** Access token sai chữ ký / hết hạn / thiếu claim. */
    public static class InvalidAccessTokenException extends RuntimeException {
        public InvalidAccessTokenException(String message) {
            super(message);
        }
    }
}
