package com.cardapio.identity.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.identity.api.dto.ProfileResponse;
import com.cardapio.identity.api.dto.UpdateProfileRequest;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.application.usecase.GetMyProfileUseCase;
import com.cardapio.identity.application.usecase.UpdateMyProfileUseCase;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final GetMyProfileUseCase getMy;
    private final UpdateMyProfileUseCase updateMy;

    public MeController(GetMyProfileUseCase getMy, UpdateMyProfileUseCase updateMy) {
        this.getMy = getMy; this.updateMy = updateMy;
    }

    @GetMapping
    public ResponseEntity<?> me(@AuthenticationPrincipal CardapioPrincipal principal) {
        if (principal == null || principal.audience() != Audience.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        Result<CustomerProfile> r = getMy.execute(CustomerId.of(principal.subject()));
        return switch (r) {
            case Result.Success<CustomerProfile> s -> ResponseEntity.ok(ProfileResponse.from(s.value()));
            case Result.Failure<CustomerProfile> f -> ResponseEntity.status(404)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }

    @PutMapping
    public ResponseEntity<?> update(@AuthenticationPrincipal CardapioPrincipal principal,
                                    @Valid @RequestBody UpdateProfileRequest req) {
        if (principal == null || principal.audience() != Audience.CUSTOMER) {
            return ResponseEntity.status(403).build();
        }
        Result<CustomerProfile> r = updateMy.execute(new UpdateProfileCommand(
            CustomerId.of(principal.subject()), req.name(), req.phoneNumber()));
        return switch (r) {
            case Result.Success<CustomerProfile> s -> ResponseEntity.ok(ProfileResponse.from(s.value()));
            case Result.Failure<CustomerProfile> f -> ResponseEntity.unprocessableEntity()
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(ProblemDetails.fromNotification(f.notification()));
        };
    }
}
