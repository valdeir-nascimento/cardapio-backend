package com.cardapio.identity.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.api.error.UnauthorizedException;
import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.application.command.LoginWithSocialIdentityCommand;
import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.application.usecase.*;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.shared.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IdentityFacadeImpl implements IdentityFacade {

    private final RegisterCustomerUseCase register;
    private final LoginCustomerUseCase loginCustomer;
    private final LoginAdminUseCase loginAdmin;
    private final LoginWithSocialIdentityUseCase loginWithSocial;
    private final RefreshTokenUseCase refresh;
    private final GetMyProfileUseCase getMy;
    private final UpdateMyProfileUseCase updateMy;

    @Override
    public CustomerId registerCustomer(RegisterCustomerCommand cmd) {
        return register.execute(cmd).orElseThrow(NotificationException::new);
    }

    @Override
    public TokenPair loginCustomer(LoginCommand cmd) {
        return loginCustomer.execute(cmd).orElseThrow(UnauthorizedException::new);
    }

    @Override
    public TokenPair loginAdmin(LoginCommand cmd) {
        return loginAdmin.execute(cmd).orElseThrow(UnauthorizedException::new);
    }

    @Override
    public TokenPair loginWithSocial(LoginWithSocialIdentityCommand cmd) {
        return loginWithSocial.execute(cmd).orElseThrow(UnauthorizedException::new);
    }

    @Override
    public TokenPair refresh(RefreshTokenCommand cmd) {
        return refresh.execute(cmd).orElseThrow(UnauthorizedException::new);
    }

    @Override
    public CustomerProfile getMyProfile(CustomerId id) {
        return getMy.execute(id).orElseThrow(NotFoundException::new);
    }

    @Override
    public CustomerProfile updateMyProfile(UpdateProfileCommand cmd) {
        return updateMy.execute(cmd).orElseThrow(IdentityFacadeImpl::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
