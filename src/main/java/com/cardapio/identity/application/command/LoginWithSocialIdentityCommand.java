package com.cardapio.identity.application.command;

import com.cardapio.identity.domain.model.SocialProvider;

public record LoginWithSocialIdentityCommand(SocialProvider provider, String idToken) {}
