package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.SocialLoginRequest;
import com.cardapio.identity.api.dto.TokenPairResponse;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.command.LoginWithSocialIdentityCommand;
import com.cardapio.identity.domain.model.SocialProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/social")
@RequiredArgsConstructor
public class SocialAuthController implements SocialAuthApi {

    private final IdentityFacade identity;

    @Override
    @PostMapping("/google")
    public TokenPairResponse google(SocialLoginRequest body) {
        return TokenPairResponse.from(identity.loginWithSocial(
            new LoginWithSocialIdentityCommand(SocialProvider.GOOGLE, body.idToken())));
    }

    @Override
    @PostMapping("/apple")
    public TokenPairResponse apple(SocialLoginRequest body) {
        return TokenPairResponse.from(identity.loginWithSocial(
            new LoginWithSocialIdentityCommand(SocialProvider.APPLE, body.idToken())));
    }
}
