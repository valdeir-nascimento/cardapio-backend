package com.cardapio.modulith;

import com.cardapio.CardapioApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ModulithDocumentationTest {

    @Test
    void writeModuleDocumentation() {
        ApplicationModules modules = ApplicationModules.of(CardapioApplication.class);
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
