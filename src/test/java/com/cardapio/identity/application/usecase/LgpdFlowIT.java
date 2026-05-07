package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.DeleteMyAccountCommand;
import com.cardapio.identity.application.command.LoginCommand;
import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.application.dto.CustomerDataExport;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.TokenPair;
import com.cardapio.shared.domain.Result;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class LgpdFlowIT {

    @Autowired RegisterCustomerUseCase register;
    @Autowired LoginCustomerUseCase login;
    @Autowired DeleteMyAccountUseCase deleteAccount;
    @Autowired ExportMyDataUseCase exportData;

    @Test
    void registerExportDeleteAndRefuseLogin() {
        String suffix = Integer.toHexString(Math.abs((int) System.nanoTime()));
        String email = "lgpd-" + suffix + "@example.com";
        String password = "S3cure-Pass-1!";

        Result<CustomerId> registered = register.execute(new RegisterCustomerCommand(
            "Maria Silva", email, "+5511912345678", password));
        assertThat(registered.isSuccess()).isTrue();
        CustomerId id = registered.getOrThrow();

        // Pre-delete export
        Result<CustomerDataExport> beforeDelete = exportData.execute(id);
        assertThat(beforeDelete.isSuccess()).isTrue();
        CustomerDataExport before = beforeDelete.getOrThrow();
        assertThat(before.email()).isEqualTo(email);
        assertThat(before.deletedAt()).isNull();

        // Login still works
        Result<TokenPair> beforeLogin = login.execute(new LoginCommand(email, password));
        assertThat(beforeLogin.isSuccess()).isTrue();

        // Delete the account
        Result<Void> deleted = deleteAccount.execute(new DeleteMyAccountCommand(id));
        assertThat(deleted.isSuccess()).isTrue();

        // Post-delete export still works (returns anonymized profile)
        Result<CustomerDataExport> afterDelete = exportData.execute(id);
        assertThat(afterDelete.isSuccess()).isTrue();
        CustomerDataExport after = afterDelete.getOrThrow();
        assertThat(after.email()).startsWith("deleted+").endsWith("@cardapio.local");
        assertThat(after.deletedAt()).isNotNull();
        assertThat(after.phoneNumber()).isNull();

        // Login with the original credentials is denied
        Result<TokenPair> afterLogin = login.execute(new LoginCommand(email, password));
        assertThat(afterLogin.isSuccess()).isFalse();
        assertThat(((Result.Failure<TokenPair>) afterLogin).notification().errors())
            .extracting("code").contains("INVALID_CREDENTIALS");
    }
}
