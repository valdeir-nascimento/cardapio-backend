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

class WppConnectWhatsAppSenderTest {

    private static final String BASE_URL = "http://localhost:21465";
    private static final String SEND_URL = BASE_URL + "/api/cardapio/send-message";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private WppConnectWhatsAppSender sender;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        WppConnectProperties props = new WppConnectProperties(true, "cardapio", "TOK", BASE_URL);
        sender = new WppConnectWhatsAppSender(builder.build(), props);
    }

    @Test
    void sendsToSessionEndpoint() {
        server.expect(requestTo(SEND_URL))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(jsonPath("$.phone").value("5511999998888"))
            .andExpect(jsonPath("$.isGroup").value(false))
            .andExpect(jsonPath("$.message").value("oi"))
            .andRespond(withSuccess());

        sender.sendText("+5511999998888", "oi");

        server.verify();
    }

    @Test
    void normalizesPunctuationFromPhone() {
        assertThat(WppConnectWhatsAppSender.normalize("+55 (11) 99999-8888")).isEqualTo("5511999998888");
        assertThat(WppConnectWhatsAppSender.normalize("5511999998888")).isEqualTo("5511999998888");
    }

    @Test
    void rejectsBlankPhone() {
        assertThatThrownBy(() -> WppConnectWhatsAppSender.normalize(""))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> WppConnectWhatsAppSender.normalize("---"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mapsServerErrorTo5xxCode() {
        server.expect(requestTo(SEND_URL))
            .andRespond(withServerError().body("boom"));

        assertThatThrownBy(() -> sender.sendText("+5511999998888", "x"))
            .isInstanceOf(NotificationDispatchException.class)
            .satisfies(e -> assertThat(((NotificationDispatchException) e).code()).isEqualTo("WPPCONNECT_5XX"));
    }

    @Test
    void mapsClientErrorTo4xxCode() {
        server.expect(requestTo(SEND_URL))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("nope"));

        assertThatThrownBy(() -> sender.sendText("+5511999998888", "x"))
            .isInstanceOf(NotificationDispatchException.class)
            .satisfies(e -> assertThat(((NotificationDispatchException) e).code()).isEqualTo("WPPCONNECT_4XX"));
    }
}
