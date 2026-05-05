package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

@Service
public class LoginCustomerUseCase {

    private final CustomerRepository customers;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordHasher hasher;
    private final JwtIssuer issuer;
    private final Clock clock;

    public LoginCustomerUseCase(CustomerRepository customers, RefreshTokenRepository refreshTokens,
                                PasswordHasher hasher, JwtIssuer issuer, Clock clock) {
        this.customers = customers; this.refreshTokens = refreshTokens;
        this.hasher = hasher; this.issuer = issuer; this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(LoginCommand cmd) {
        Notification n = Notification.empty();
        Email email;
        try { email = Email.of(cmd.email()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Optional<Customer> maybeCustomer = customers.findByEmail(email);
        if (maybeCustomer.isEmpty()) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Customer customer = maybeCustomer.get();
        RawPassword raw;
        try { raw = RawPassword.of(cmd.rawPassword()); }
        catch (RuntimeException e) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        if (!hasher.matches(raw, customer.passwordHash())) {
            n.addError("INVALID_CREDENTIALS", "credenciais inválidas");
            return Result.failure(n);
        }

        Instant now = clock.instant();
        var access = issuer.issueAccessToken(customer.id().value(), Audience.CUSTOMER, Set.of());
        String rawRefresh = issuer.generateOpaqueRefreshToken();
        Instant refreshExp = issuer.refreshTokenExpiry(now);
        Duration refreshTtl = Duration.between(now, refreshExp);

        RefreshToken token = RefreshToken.issue(
            customer.id().value(), Audience.CUSTOMER,
            sha256Hex(rawRefresh), now, refreshTtl);
        refreshTokens.save(token);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawRefresh, refreshExp));
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
