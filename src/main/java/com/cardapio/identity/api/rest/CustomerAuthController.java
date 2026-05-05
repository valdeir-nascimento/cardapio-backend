package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.LoginRequest;
import com.cardapio.identity.api.dto.RefreshRequest;
import com.cardapio.identity.api.dto.RegisterRequest;
import com.cardapio.identity.api.dto.TokenPairResponse;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.domain.model.CustomerId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class CustomerAuthController {

    private final IdentityFacade identity;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req) {
        CustomerId id = identity.registerCustomer(new RegisterCustomerCommand(req.name(), req.email(), req.phoneNumber(), req.password()));
        return ResponseEntity.created(URI.create("/api/v1/me")).body(Map.of("id", id.value()));
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest req) {
        return TokenPairResponse.from(identity.loginCustomer(new LoginCommand(req.email(), req.password())));
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return TokenPairResponse.from(identity.refresh(new RefreshTokenCommand(req.refreshToken())));
    }
}
