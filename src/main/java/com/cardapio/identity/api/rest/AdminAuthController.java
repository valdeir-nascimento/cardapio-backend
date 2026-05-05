package com.cardapio.identity.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.identity.api.dto.LoginRequest;
import com.cardapio.identity.api.dto.TokenPairResponse;
import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.application.usecase.LoginAdminUseCase;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final LoginAdminUseCase login;

    public AdminAuthController(LoginAdminUseCase login) { this.login = login; }

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
}
