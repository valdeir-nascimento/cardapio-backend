package com.cardapio.modulith;

import com.cardapio.CardapioApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithVerificationTest {

    @Test
    void verifyModuleStructure() {
        ApplicationModules modules = ApplicationModules.of(CardapioApplication.class);
        modules.verify();
    }
}
