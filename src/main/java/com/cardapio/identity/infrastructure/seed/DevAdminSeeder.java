package com.cardapio.identity.infrastructure.seed;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
public class DevAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);
    private static final Email DEFAULT_EMAIL = Email.of("admin@cardapio.local");
    private static final String DEFAULT_PASSWORD = "Admin@123!";

    private final AdminRepository admins;
    private final PasswordHasher hasher;

    public DevAdminSeeder(AdminRepository admins, PasswordHasher hasher) {
        this.admins = admins;
        this.hasher = hasher;
    }

    @EventListener
    public void seed(ApplicationReadyEvent event) {
        if (admins.findByEmail(DEFAULT_EMAIL).isPresent()) return;
        Admin admin = Admin.create("Admin Dev", DEFAULT_EMAIL,
            hasher.hash(RawPassword.of(DEFAULT_PASSWORD)), Set.of(Role.OWNER));
        admins.save(admin);
        log.warn("DEV admin seeded: email={} password={}  (only in dev profile)", DEFAULT_EMAIL.value(), DEFAULT_PASSWORD);
    }
}
