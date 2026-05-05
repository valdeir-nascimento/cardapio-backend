package com.cardapio.api.error;

import com.cardapio.shared.domain.DomainException;
import com.cardapio.shared.domain.Notification;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgresTestContainerConfig.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void translatesDomainExceptionTo422WithProblemJson() throws Exception {
        mvc.perform(get("/__test__/domain-exception"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
            .andExpect(jsonPath("$.code").value("SAMPLE_RULE"))
            .andExpect(jsonPath("$.detail").value("regra rompida"));
    }

    @Test
    void translatesNotificationTo422WithErrorsArray() throws Exception {
        mvc.perform(get("/__test__/notification"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.errors[0].code").value("OUT_OF_STOCK"))
            .andExpect(jsonPath("$.errors[0].message").value("esgotado"));
    }

    @Test
    void translatesUnexpectedExceptionTo500() throws Exception {
        mvc.perform(get("/__test__/boom"))
            .andExpect(status().isInternalServerError())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(500));
    }

    @RestController
    static class TestController {

        static class SampleException extends DomainException {
            SampleException() { super("SAMPLE_RULE", "regra rompida"); }
        }

        @GetMapping("/__test__/domain-exception")
        void domainEx() { throw new SampleException(); }

        @GetMapping("/__test__/notification")
        Object notification() {
            Notification n = Notification.empty();
            n.addError("OUT_OF_STOCK", "esgotado");
            throw new com.cardapio.api.error.NotificationException(n);
        }

        @GetMapping("/__test__/boom")
        void boom() { throw new RuntimeException("boom"); }
    }
}
