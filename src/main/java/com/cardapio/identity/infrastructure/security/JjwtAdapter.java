package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.JwtVerifier;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JjwtAdapter implements JwtIssuer, JwtVerifier {

    private final JwtProperties props;
    private final Clock clock;
    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public JjwtAdapter(JwtProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public IssuedJwt issueAccessToken(UUID subject, Audience audience, Set<Role> roles) {
        Instant now = clock.instant();
        Instant exp = now.plus(props.accessTokenTtl());
        String token = Jwts.builder()
            .subject(subject.toString())
            .audience().add(audience.name()).and()
            .claim("roles", roles.stream().map(Role::name).toList())
            .issuedAt(Date.from(now))
            .expiration(Date.from(exp))
            .signWith(key, Jwts.SIG.HS256)
            .compact();
        return new IssuedJwt(token, exp);
    }

    @Override
    public String generateOpaqueRefreshToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public Instant accessTokenExpiry(Instant now) { return now.plus(props.accessTokenTtl()); }

    @Override
    public Instant refreshTokenExpiry(Instant now) { return now.plus(props.refreshTokenTtl()); }

    @Override
    public VerifiedJwt verify(String token) {
        try {
            var claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

            UUID subject = UUID.fromString(claims.getSubject());
            Audience audience = Audience.valueOf(claims.getAudience().iterator().next());
            @SuppressWarnings("unchecked")
            List<String> roleNames = (List<String>) claims.getOrDefault("roles", List.of());
            Set<Role> roles = roleNames.stream().map(Role::valueOf).collect(Collectors.toUnmodifiableSet());
            return new VerifiedJwt(subject, audience, roles);
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("invalid JWT: " + e.getMessage(), e);
        }
    }
}
