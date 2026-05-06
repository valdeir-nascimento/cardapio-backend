package com.cardapio.identity.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.api.error.UnauthorizedException;
import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.application.command.RefreshTokenCommand;
import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.TokenPair;
import org.springframework.modulith.NamedInterface;

@NamedInterface("IdentityFacade")
public interface IdentityFacade {

    CustomerId registerCustomer(RegisterCustomerCommand cmd) throws NotificationException;

    TokenPair loginCustomer(LoginCommand cmd) throws UnauthorizedException;

    TokenPair loginAdmin(LoginCommand cmd) throws UnauthorizedException;

    TokenPair refresh(RefreshTokenCommand cmd) throws UnauthorizedException;

    CustomerProfile getMyProfile(CustomerId id) throws NotFoundException;

    CustomerProfile updateMyProfile(UpdateProfileCommand cmd) throws NotificationException;
}
