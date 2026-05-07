package com.cardapio.identity.domain.exception;

import com.cardapio.identity.domain.model.SocialProvider;

public class SocialIdentityAlreadyLinkedException extends RuntimeException {
    public SocialIdentityAlreadyLinkedException(SocialProvider provider) {
        super("a different " + provider + " identity is already linked to this customer");
    }
}
