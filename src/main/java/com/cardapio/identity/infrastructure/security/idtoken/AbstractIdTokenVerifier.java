package com.cardapio.identity.infrastructure.security.idtoken;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cardapio.identity.domain.port.IdTokenVerifier;
import com.cardapio.identity.infrastructure.security.jwks.CachedJwksProvider;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;

import static com.cardapio.identity.domain.port.IdTokenVerifier.InvalidIdTokenException.Reason.*;

abstract class AbstractIdTokenVerifier implements IdTokenVerifier {

    private final CachedJwksProvider jwks;
    private final String jwksUri;
    private final Set<String> acceptedIssuers;
    private final String expectedAudience;

    protected AbstractIdTokenVerifier(CachedJwksProvider jwks, String jwksUri,
                                      Set<String> acceptedIssuers, String expectedAudience) {
        this.jwks = jwks;
        this.jwksUri = jwksUri;
        this.acceptedIssuers = acceptedIssuers;
        this.expectedAudience = expectedAudience;
    }

    @Override
    public final VerifiedIdToken verify(String idToken) {
        DecodedJWT decoded = decode(idToken);
        PublicKey key = resolveKey(decoded);
        verifySignature(decoded, key);
        verifyClaims(decoded);
        afterVerify(decoded);

        String subject = decoded.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidIdTokenException(MALFORMED, "missing subject claim");
        }
        String email = decoded.getClaim("email").asString();
        String name = extractName(decoded);
        return new VerifiedIdToken(subject, email, name);
    }

    /** Hook for provider-specific extra checks (e.g. Google email_verified). */
    protected void afterVerify(DecodedJWT decoded) {}

    /** Provider may override to fall back to given_name / family_name. */
    protected String extractName(DecodedJWT decoded) {
        String name = decoded.getClaim("name").asString();
        if (name != null && !name.isBlank()) return name;
        String given = decoded.getClaim("given_name").asString();
        String family = decoded.getClaim("family_name").asString();
        if (given == null && family == null) return null;
        return ((given == null ? "" : given) + " " + (family == null ? "" : family)).trim();
    }

    private DecodedJWT decode(String idToken) {
        try {
            return JWT.decode(idToken);
        } catch (RuntimeException e) {
            throw new InvalidIdTokenException(MALFORMED, "cannot decode id token", e);
        }
    }

    private PublicKey resolveKey(DecodedJWT decoded) {
        String kid = decoded.getKeyId();
        if (kid == null || kid.isBlank()) {
            throw new InvalidIdTokenException(MALFORMED, "missing kid header");
        }
        try {
            return jwks.getPublicKey(jwksUri, kid);
        } catch (RuntimeException e) {
            throw new InvalidIdTokenException(INVALID_SIGNATURE, "cannot resolve signing key: " + e.getMessage(), e);
        }
    }

    private void verifySignature(DecodedJWT decoded, PublicKey key) {
        String alg = decoded.getAlgorithm();
        try {
            Algorithm algorithm = switch (alg) {
                case "RS256" -> Algorithm.RSA256((RSAPublicKey) key, null);
                case "ES256" -> Algorithm.ECDSA256((ECPublicKey) key, null);
                default -> throw new InvalidIdTokenException(MALFORMED, "unsupported algorithm: " + alg);
            };
            JWT.require(algorithm).build().verify(decoded);
        } catch (TokenExpiredException e) {
            throw new InvalidIdTokenException(EXPIRED, "token expired", e);
        } catch (AlgorithmMismatchException e) {
            throw new InvalidIdTokenException(MALFORMED, "algorithm mismatch", e);
        } catch (SignatureVerificationException e) {
            throw new InvalidIdTokenException(INVALID_SIGNATURE, "signature verification failed", e);
        } catch (InvalidClaimException e) {
            throw new InvalidIdTokenException(MALFORMED, "invalid claim: " + e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new InvalidIdTokenException(MALFORMED, "key/algorithm mismatch", e);
        }
    }

    private void verifyClaims(DecodedJWT decoded) {
        String issuer = decoded.getIssuer();
        if (issuer == null || acceptedIssuers.stream().noneMatch(i -> i.equals(issuer))) {
            throw new InvalidIdTokenException(INVALID_ISSUER, "issuer not accepted: " + issuer);
        }
        if (!decoded.getAudience().contains(expectedAudience)) {
            throw new InvalidIdTokenException(INVALID_AUDIENCE,
                "audience mismatch; expected " + expectedAudience);
        }
    }
}
