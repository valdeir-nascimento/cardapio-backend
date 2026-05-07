package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.ProfileResponse;
import com.cardapio.identity.api.dto.UpdateProfileRequest;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.command.DeleteMyAccountCommand;
import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.domain.model.CustomerId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class MeController {

    private final IdentityFacade identity;

    @GetMapping
    public ProfileResponse me(@AuthenticationPrincipal CardapioPrincipal principal) {
        return ProfileResponse.from(identity.getMyProfile(CustomerId.of(principal.subject())));
    }

    @PutMapping
    public ProfileResponse update(@AuthenticationPrincipal CardapioPrincipal principal, @Valid @RequestBody UpdateProfileRequest req) {
        return ProfileResponse.from(identity.updateMyProfile(new UpdateProfileCommand(CustomerId.of(principal.subject()), req.name(), req.phoneNumber())));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CardapioPrincipal principal) {
        identity.deleteMyAccount(new DeleteMyAccountCommand(CustomerId.of(principal.subject())));
    }
}
