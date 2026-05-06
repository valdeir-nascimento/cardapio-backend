package com.cardapio.notification.infrastructure.template;

import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.domain.port.TemplateRenderer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafTemplateRendererTest {

    private final ThymeleafTemplateRenderer renderer = new ThymeleafTemplateRenderer();

    private static Map<String, Object> sampleModel() {
        return Map.of(
            "customer", Map.of("name", "Maria"),
            "order", Map.of(
                "shortRef", "AB12CD",
                "modality", "DELIVERY",
                "status", "CONFIRMED",
                "previousStatus", "RECEIVED",
                "total", "42.50"
            )
        );
    }

    @Test
    void rendersOrderReceivedEmail() {
        TemplateRenderer.EmailContent c = renderer.renderEmail(NotificationTemplate.ORDER_RECEIVED, sampleModel());
        assertThat(c.subject()).isEqualTo("Recebemos seu pedido #AB12CD");
        assertThat(c.html()).contains("Maria").contains("AB12CD").contains("DELIVERY").contains("42.50");
    }

    @Test
    void rendersOrderStatusChangedEmail() {
        TemplateRenderer.EmailContent c = renderer.renderEmail(NotificationTemplate.ORDER_STATUS_CHANGED, sampleModel());
        assertThat(c.subject()).isEqualTo("Pedido #AB12CD: CONFIRMED");
        assertThat(c.html()).contains("RECEIVED").contains("CONFIRMED");
    }

    @Test
    void rendersOrderCanceledEmail() {
        TemplateRenderer.EmailContent c = renderer.renderEmail(NotificationTemplate.ORDER_CANCELED, sampleModel());
        assertThat(c.subject()).isEqualTo("Pedido #AB12CD cancelado");
        assertThat(c.html()).contains("cancelado");
    }

    @Test
    void rendersPaymentApprovedEmail() {
        TemplateRenderer.EmailContent c = renderer.renderEmail(NotificationTemplate.PAYMENT_APPROVED, sampleModel());
        assertThat(c.subject()).isEqualTo("Pagamento aprovado — pedido #AB12CD");
        assertThat(c.html()).contains("aprovado").contains("42.50");
    }

    @Test
    void rendersPaymentRejectedEmail() {
        TemplateRenderer.EmailContent c = renderer.renderEmail(NotificationTemplate.PAYMENT_REJECTED, sampleModel());
        assertThat(c.subject()).isEqualTo("Falha no pagamento — pedido #AB12CD");
        assertThat(c.html()).contains("recusado");
    }

    @Test
    void rendersWhatsAppOrderReceived() {
        String body = renderer.renderWhatsApp(NotificationTemplate.ORDER_RECEIVED, sampleModel());
        assertThat(body).contains("Maria").contains("AB12CD").contains("DELIVERY").contains("42.50");
    }

    @Test
    void rendersWhatsAppStatusChanged() {
        String body = renderer.renderWhatsApp(NotificationTemplate.ORDER_STATUS_CHANGED, sampleModel());
        assertThat(body).contains("AB12CD").contains("CONFIRMED");
    }
}
