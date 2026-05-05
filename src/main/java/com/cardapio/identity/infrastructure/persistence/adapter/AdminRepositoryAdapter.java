package com.cardapio.identity.infrastructure.persistence.adapter;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.AdminId;
import com.cardapio.identity.domain.port.AdminRepository;
import com.cardapio.identity.infrastructure.persistence.mapper.AdminMapper;
import com.cardapio.identity.infrastructure.persistence.repository.SpringAdminJpaRepository;
import com.cardapio.shared.domain.Email;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class AdminRepositoryAdapter implements AdminRepository {

    private final SpringAdminJpaRepository jpa;
    private final Clock clock;

    public AdminRepositoryAdapter(SpringAdminJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void save(Admin admin) {
        var existing = jpa.findById(admin.id().value());
        if (existing.isPresent()) {
            AdminMapper.updateJpa(existing.get(), admin);
            jpa.save(existing.get());
        } else {
            jpa.save(AdminMapper.toJpa(admin, clock.instant()));
        }
    }

    @Override
    public Optional<Admin> findById(AdminId id) {
        return jpa.findById(id.value()).map(AdminMapper::toDomain);
    }

    @Override
    public Optional<Admin> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(AdminMapper::toDomain);
    }
}
