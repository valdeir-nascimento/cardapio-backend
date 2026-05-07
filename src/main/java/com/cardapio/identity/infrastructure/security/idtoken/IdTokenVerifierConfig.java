package com.cardapio.identity.infrastructure.security.idtoken;

import com.cardapio.identity.domain.port.IdTokenVerifier;
import com.cardapio.identity.infrastructure.security.jwks.CachedJwksProvider;
import com.cardapio.identity.infrastructure.security.jwks.JwksClient;
import com.cardapio.identity.infrastructure.security.jwks.JwksProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({OAuthProperties.class, JwksProperties.class})
public class IdTokenVerifierConfig {

    @Bean
    JwksClient jwksClient(JwksProperties props) {
        // RestClient uses the default HTTP client; we don't need to swap it for the timeouts here.
        return new JwksClient(RestClient.builder().build());
    }

    @Bean
    CachedJwksProvider cachedJwksProvider(JwksClient client, JwksProperties props, Clock clock) {
        return new CachedJwksProvider(client, props.cacheTtl(), clock);
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth.google", name = "enabled", havingValue = "true")
    IdTokenVerifier googleIdTokenVerifier(CachedJwksProvider jwks, OAuthProperties props) {
        return new GoogleIdTokenVerifier(jwks, props.google());
    }

    @Bean
    @ConditionalOnProperty(prefix = "oauth.apple", name = "enabled", havingValue = "true")
    IdTokenVerifier appleIdTokenVerifier(CachedJwksProvider jwks, OAuthProperties props) {
        return new AppleIdTokenVerifier(jwks, props.apple());
    }
}
