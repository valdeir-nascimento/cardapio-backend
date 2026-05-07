package com.cardapio.api.support.ratelimit;

import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
@TestPropertySource(properties = {
    "rate-limit.enabled=true",
    "rate-limit.rules[0].paths[0]=/api/v1/auth/login",
    "rate-limit.rules[0].methods[0]=POST",
    "rate-limit.rules[0].capacity=10",
    "rate-limit.rules[0].refill-tokens=10",
    "rate-limit.rules[0].refill-period=1m"
})
@DirtiesContext
class RateLimitIT {

    @Autowired MockMvc mvc;

    @Test
    void elevenLoginsFromSameIpYieldA429OnTheEleventh() throws Exception {
        String body = """
            {"email":"nobody-rate-limit@example.com","password":"WrongPass!1"}
            """;
        String clientIp = "203.0.113.42";

        for (int i = 1; i <= 10; i++) {
            int status = mvc.perform(post("/api/v1/auth/login")
                    .header("X-Forwarded-For", clientIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andReturn().getResponse().getStatus();
            assertThat(status)
                .as("request %d should not be rate-limited", i)
                .isNotEqualTo(429);
        }

        MvcResult eleventh = mvc.perform(post("/api/v1/auth/login")
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andReturn();

        assertThat(eleventh.getResponse().getStatus()).isEqualTo(429);
        assertThat(eleventh.getResponse().getHeader("Retry-After")).isNotBlank();
        assertThat(eleventh.getResponse().getContentAsString()).contains("RATE_LIMITED");
    }
}
