package com.cardapio.notification.infrastructure.whatsapp;

import com.cardapio.notification.domain.exception.NotificationDispatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ZapiWhatsAppSenderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private ZapiWhatsAppSender sender;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("https://api.z-api.io");
        server = MockRestServiceServer.bindTo(builder).build();
        ZapiProperties props = new ZapiProperties(true, "INST", "TOK", "CLI", "https://api.z-api.io");
        sender = new ZapiWhatsAppSender(builder.build(), props);
    }

    @Test
    void sendsToInstanceTokenEndpoint() {
        server.expect(requestTo("https://api.z-api.io/instances/INST/token/TOK/send-text"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(jsonPath("$.phone").value("5511999998888"))
            .andExpect(jsonPath("$.message").value("oi"))
            .andRespond(withSuccess());

        sender.sendText("+5511999998888", "oi");

        server.verify();
    }

    @Test
    void normalizesPunctuationFromPhone() {
        assertThat(ZapiWhatsAppSender.normalize("+55 (11) 99999-8888")).isEqualTo("5511999998888");
        assertThat(ZapiWhatsAppSender.normalize("5511999998888")).isEqualTo("5511999998888");
    }

    @Test
    void rejectsBlankPhone() {
        assertThatThrownBy(() -> ZapiWhatsAppSender.normalize(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ZapiWhatsAppSender.normalize("---"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsServerErrorTo5xxCode() {
        server.expect(requestTo("https://api.z-api.io/instances/INST/token/TOK/send-text"))
            .andRespond(withServerError().body("boom"));

        assertThatThrownBy(() -> sender.sendText("+5511999998888", "x"))
            .isInstanceOf(NotificationDispatchException.class)
            .satisfies(e -> assertThat(((NotificationDispatchException) e).code()).isEqualTo("ZAPI_5XX"));
    }

    @Test
    void mapsClientErrorTo4xxCode() {
        server.expect(requestTo("https://api.z-api.io/instances/INST/token/TOK/send-text"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("nope"));

        assertThatThrownBy(() -> sender.sendText("+5511999998888", "x"))
            .isInstanceOf(NotificationDispatchException.class)
            .satisfies(e -> assertThat(((NotificationDispatchException) e).code()).isEqualTo("ZAPI_4XX"));
    }
}
