package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.LoginRequest;
import com.cardapio.identity.api.dto.TokenPairResponse;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.command.LoginCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController implements AdminAuthApi {

    private final IdentityFacade identity;

    @Override
    @PostMapping("/login")
    public TokenPairResponse login(LoginRequest req) {
        return TokenPairResponse.from(identity.loginAdmin(new LoginCommand(req.email(), req.password())));
    }
}
