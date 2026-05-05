package com.cardapio.identity.application.usecase;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.*;
import com.cardapio.shared.domain.Email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class LoginAdminUseCase extends AbstractLoginUseCase<Admin> {

    private final AdminRepository admins;

    public LoginAdminUseCase(
        AdminRepository admins,
        PasswordHasher hasher,
        JwtIssuer issuer,
        RefreshTokenRepository refreshTokens,
        TokenHasher tokenHasher,
        Clock clock
    ) {
        super(hasher, issuer, refreshTokens, tokenHasher, clock);
        this.admins = admins;
    }

    @Override
    @Transactional
    protected Optional<Admin> findUser(Email email) {
        return admins.findByEmail(email);
    }

    @Override
    protected HashedPassword passwordOf(Admin u) {
        return u.passwordHash();
    }

    @Override
    protected UUID subjectOf(Admin u) {
        return u.id().value();
    }

    @Override
    protected Audience audienceOf(Admin u) {
        return Audience.ADMIN;
    }

    @Override
    protected Set<Role> rolesOf(Admin u) {
        return u.roles();
    }
}
