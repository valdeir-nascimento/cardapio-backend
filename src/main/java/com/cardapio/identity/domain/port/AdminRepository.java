package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.AdminId;
import com.cardapio.shared.domain.Email;

import java.util.Optional;

public interface AdminRepository {
    void save(Admin admin);
    Optional<Admin> findById(AdminId id);
    Optional<Admin> findByEmail(Email email);
}
