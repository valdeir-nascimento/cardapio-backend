package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.SocialProvider;

public interface IdTokenVerifier {

    SocialProvider provider();

    VerifiedIdToken verify(String idToken) throws InvalidIdTokenException;

    record VerifiedIdToken(String subject, String email, String name) {}

    class InvalidIdTokenException extends RuntimeException {
        public enum Reason { EXPIRED, INVALID_SIGNATURE, INVALID_ISSUER, INVALID_AUDIENCE, MALFORMED, EMAIL_NOT_VERIFIED }

        private final Reason reason;

        public InvalidIdTokenException(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public InvalidIdTokenException(Reason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        public Reason reason() { return reason; }
    }
}
