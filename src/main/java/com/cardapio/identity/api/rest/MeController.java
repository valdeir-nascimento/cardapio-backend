package com.cardapio.identity.api.rest;

import com.cardapio.identity.api.dto.ProfileResponse;
import com.cardapio.identity.api.dto.UpdateProfileRequest;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.command.DeleteMyAccountCommand;
import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerDataExport;
import com.cardapio.identity.domain.model.CustomerId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("hasRole('CUSTOMER')")
@RequiredArgsConstructor
public class MeController implements MeApi {

    private final IdentityFacade identity;

    @Override
    @GetMapping
    public ProfileResponse me(@AuthenticationPrincipal CardapioPrincipal principal) {
        return ProfileResponse.from(identity.getMyProfile(CustomerId.of(principal.subject())));
    }

    @Override
    @PutMapping
    public ProfileResponse update(@AuthenticationPrincipal CardapioPrincipal principal,
                                  UpdateProfileRequest req) {
        return ProfileResponse.from(identity.updateMyProfile(
            new UpdateProfileCommand(CustomerId.of(principal.subject()), req.name(), req.phoneNumber())));
    }

    @Override
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CardapioPrincipal principal) {
        identity.deleteMyAccount(new DeleteMyAccountCommand(CustomerId.of(principal.subject())));
    }

    @Override
    @GetMapping("/data-export")
    public ResponseEntity<CustomerDataExport> dataExport(@AuthenticationPrincipal CardapioPrincipal principal) {
        CustomerDataExport export = identity.exportMyData(CustomerId.of(principal.subject()));
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"cardapio-export-" + export.id() + ".json\"")
            .body(export);
    }
}
