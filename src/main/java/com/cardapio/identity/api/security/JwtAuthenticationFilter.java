package com.cardapio.identity.api.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.port.JwtVerifier;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtVerifier verifier;

    public JwtAuthenticationFilter(JwtVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                JwtVerifier.VerifiedJwt verified = verifier.verify(token);
                CardapioPrincipal principal = new CardapioPrincipal(
                    verified.subject(), verified.audience(), verified.roles());
                List<SimpleGrantedAuthority> authorities = new ArrayList<>(verified.roles().stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                    .toList());
                if (verified.audience() == Audience.CUSTOMER) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
                }
                var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException ignored) {
                // invalid token → proceed unauthenticated; Spring Security rejects if endpoint needs auth
            }
        }
        chain.doFilter(req, res);
    }
}
