package com.cardapio.identity.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.identity.api.dto.*;
import com.cardapio.identity.application.command.*;
import com.cardapio.identity.application.usecase.LoginCustomerUseCase;
import com.cardapio.identity.application.usecase.RefreshTokenUseCase;
import com.cardapio.identity.application.usecase.RegisterCustomerUseCase;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class CustomerAuthController {

    private final RegisterCustomerUseCase register;
    private final LoginCustomerUseCase login;
    private final RefreshTokenUseCase refresh;

    public CustomerAuthController(RegisterCustomerUseCase register, LoginCustomerUseCase login, RefreshTokenUseCase refresh) {
        this.register = register; this.login = login; this.refresh = refresh;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        Result<CustomerId> r = register.execute(new RegisterCustomerCommand(req.name(), req.email(), req.phoneNumber(), req.password()));
        return switch (r) {
            case Result.Success<CustomerId> s -> ResponseEntity.created(URI.create("/api/v1/me"))
                .body(Map.of("id", s.value().value()));
            case Result.Failure<CustomerId> f -> unprocessable(f);
        };
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        Result<TokenPair> r = login.execute(new LoginCommand(req.email(), req.password()));
        return switch (r) {
            case Result.Success<TokenPair> s -> ResponseEntity.ok(TokenPairResponse.from(s.value()));
            case Result.Failure<TokenPair> f -> ResponseEntity.status(401)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshRequest req) {
        Result<TokenPair> r = refresh.execute(new RefreshTokenCommand(req.refreshToken()));
        return switch (r) {
            case Result.Success<TokenPair> s -> ResponseEntity.ok(TokenPairResponse.from(s.value()));
            case Result.Failure<TokenPair> f -> ResponseEntity.status(401)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }

    private ResponseEntity<ProblemDetail> unprocessable(Result.Failure<?> f) {
        return ResponseEntity.unprocessableEntity()
            .contentType(MediaType.parseMediaType("application/problem+json"))
            .body(ProblemDetails.fromNotification(f.notification()));
    }
}
