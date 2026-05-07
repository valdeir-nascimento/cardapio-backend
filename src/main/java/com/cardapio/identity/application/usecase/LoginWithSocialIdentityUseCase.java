package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.LoginWithSocialIdentityCommand;
import com.cardapio.identity.domain.exception.SocialIdentityAlreadyLinkedException;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.RefreshToken;
import com.cardapio.identity.domain.model.SocialIdentity;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.IdTokenVerifier;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.identity.domain.port.TokenHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LoginWithSocialIdentityUseCase {

    private final Map<SocialProvider, IdTokenVerifier> verifiers;
    private final CustomerRepository customers;
    private final JwtIssuer issuer;
    private final RefreshTokenRepository refreshTokens;
    private final TokenHasher tokenHasher;
    private final Clock clock;

    public LoginWithSocialIdentityUseCase(List<IdTokenVerifier> verifiers,
                                          CustomerRepository customers,
                                          JwtIssuer issuer,
                                          RefreshTokenRepository refreshTokens,
                                          TokenHasher tokenHasher,
                                          Clock clock) {
        this.verifiers = new EnumMap<>(SocialProvider.class);
        for (IdTokenVerifier v : verifiers) {
            this.verifiers.put(v.provider(), v);
        }
        this.customers = customers;
        this.issuer = issuer;
        this.refreshTokens = refreshTokens;
        this.tokenHasher = tokenHasher;
        this.clock = clock;
    }

    @Transactional
    public Result<TokenPair> execute(LoginWithSocialIdentityCommand cmd) {
        IdTokenVerifier verifier = verifiers.get(cmd.provider());
        if (verifier == null) {
            return Result.failWith(ErrorCode.SOCIAL_LOGIN_PROVIDER_UNAVAILABLE,
                cmd.provider() + " is not enabled");
        }

        IdTokenVerifier.VerifiedIdToken verified;
        try {
            verified = verifier.verify(cmd.idToken());
        } catch (IdTokenVerifier.InvalidIdTokenException e) {
            return Result.failWith(ErrorCode.INVALID_ID_TOKEN, e.reason() + ": " + e.getMessage());
        }

        Optional<Customer> bySubject = customers.findBySocialIdentity(cmd.provider(), verified.subject());
        Customer customer;
        if (bySubject.isPresent()) {
            customer = bySubject.get();
        } else {
            if (verified.email() == null || verified.email().isBlank()) {
                return Result.failWith(ErrorCode.SOCIAL_LOGIN_EMAIL_REQUIRED);
            }
            Email email = Email.of(verified.email());
            SocialIdentity identity = new SocialIdentity(
                cmd.provider(), verified.subject(), verified.email(), clock.instant());

            Optional<Customer> byEmail = customers.findByEmail(email);
            if (byEmail.isPresent()) {
                customer = byEmail.get();
                try {
                    customer.linkSocialIdentity(identity, clock);
                } catch (SocialIdentityAlreadyLinkedException e) {
                    return Result.failWith(ErrorCode.SOCIAL_IDENTITY_ALREADY_LINKED, e.getMessage());
                }
                customers.save(customer);
            } else {
                String name = verified.name() != null && !verified.name().isBlank()
                    ? verified.name()
                    : verified.email().split("@")[0];
                customer = Customer.registerSocial(name, email, identity);
                customers.save(customer);
            }
        }

        return issueTokenPair(customer);
    }

    private Result<TokenPair> issueTokenPair(Customer customer) {
        Instant now = clock.instant();
        var access = issuer.issueAccessToken(customer.id().value(), Audience.CUSTOMER, Set.of());
        String rawRefresh = issuer.generateOpaqueRefreshToken();
        Instant refreshExp = issuer.refreshTokenExpiry(now);

        RefreshToken token = RefreshToken.issue(
            customer.id().value(), Audience.CUSTOMER,
            tokenHasher.sha256Hex(rawRefresh), now, Duration.between(now, refreshExp));
        refreshTokens.save(token);

        return Result.success(new TokenPair(access.token(), access.expiresAt(), rawRefresh, refreshExp));
    }
}
