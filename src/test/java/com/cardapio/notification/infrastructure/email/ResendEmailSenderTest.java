package com.cardapio.notification.infrastructure.email;

import com.cardapio.notification.domain.exception.NotificationDispatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailSenderTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private ResendEmailSender sender;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("https://api.resend.com");
        server = MockRestServiceServer.bindTo(builder).build();
        ResendProperties props = new ResendProperties(true, "test-key", "from@cardapio.local", "https://api.resend.com");
        sender = new ResendEmailSender(builder.build(), props);
    }

    @Test
    void sendsValidJsonPayload() {
        server.expect(requestTo("https://api.resend.com/emails"))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.from").value("from@cardapio.local"))
            .andExpect(jsonPath("$.to[0]").value("dest@example.com"))
            .andExpect(jsonPath("$.subject").value("Olá"))
            .andExpect(jsonPath("$.html").value("<b>oi</b>"))
            .andExpect(jsonPath("$.text").value("oi"))
            .andRespond(withSuccess("{\"id\":\"abc\"}", MediaType.APPLICATION_JSON));

        sender.send("dest@example.com", "Olá", "<b>oi</b>", "oi");

        server.verify();
    }

    @Test
    void omitsTextWhenNull() {
        server.expect(requestTo("https://api.resend.com/emails"))
            .andExpect(jsonPath("$.text").doesNotExist())
            .andRespond(withSuccess());

        sender.send("dest@example.com", "S", "<p/>", null);

        server.verify();
    }

    @Test
    void mapsServerErrorTo5xxCode() {
        server.expect(requestTo("https://api.resend.com/emails"))
            .andRespond(withServerError().body("boom"));

        assertThatThrownBy(() -> sender.send("a@b.com", "S", "<p/>", null))
            .isInstanceOf(NotificationDispatchException.class)
            .satisfies(ex -> assertThat(((NotificationDispatchException) ex).code()).isEqualTo("RESEND_5XX"));
    }

    @Test
    void mapsClientErrorTo4xxCode() {
        server.expect(requestTo("https://api.resend.com/emails"))
            .andRespond(withStatus(org.springframework.http.HttpStatus.BAD_REQUEST).body("bad"));

        assertThatThrownBy(() -> sender.send("a@b.com", "S", "<p/>", null))
            .isInstanceOf(NotificationDispatchException.class)
            .satisfies(ex -> assertThat(((NotificationDispatchException) ex).code()).isEqualTo("RESEND_4XX"));
    }
}
