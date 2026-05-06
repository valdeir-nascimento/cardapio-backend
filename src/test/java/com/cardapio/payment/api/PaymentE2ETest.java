package com.cardapio.payment.api;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.payment.domain.model.CardCharge;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PixCharge;
import com.cardapio.payment.domain.port.PaymentGateway;
import com.cardapio.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class PaymentE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @MockBean PaymentGateway gateway;

    @BeforeEach
    void setUp() {
        when(gateway.verifyWebhookSignature(any(), any())).thenReturn(true);
    }

    private MockHttpServletRequestBuilder asCustomer(MockHttpServletRequestBuilder rb, UUID customerId) {
        var principal = new CardapioPrincipal(customerId, Audience.CUSTOMER, Set.of());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return rb.with(authentication(auth));
    }

    private record SetupIds(UUID customerId, String orderId) {}

    @WithMockUser(roles = {"OWNER"})
    private SetupIds setupOrder(String suffix) throws Exception {
        // 1. Category
        var catBody = """
            { "name": "Pagamentos %s", "displayOrder": 1, "active": true }
            """.formatted(suffix);
        MvcResult catResult = mvc.perform(post("/api/v1/admin/categories")
                .with(authentication(adminAuth()))
                .contentType(MediaType.APPLICATION_JSON).content(catBody))
            .andExpect(status().isCreated())
            .andReturn();
        String categoryId = json.readTree(catResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Product
        String prodBody = """
            {
              "name": "Pizza Pagamento %s",
              "description": "Test",
              "basePrice": 50.00,
              "categoryId": "%s",
              "imageUrl": null,
              "allowsHalfHalf": false,
              "variations": [],
              "addOnGroups": []
            }
            """.formatted(suffix, categoryId);
        MvcResult prodResult = mvc.perform(post("/api/v1/admin/products")
                .with(authentication(adminAuth()))
                .contentType(MediaType.APPLICATION_JSON).content(prodBody))
            .andExpect(status().isCreated())
            .andReturn();
        String productId = json.readTree(prodResult.getResponse().getContentAsString()).get("id").asText();

        // 3. Neighborhood
        String nBody = """
            { "name": "Bairro %s", "city": "Salvador", "fee": 5.00, "active": true }
            """.formatted(suffix);
        MvcResult nResult = mvc.perform(post("/api/v1/admin/neighborhoods")
                .with(authentication(adminAuth()))
                .contentType(MediaType.APPLICATION_JSON).content(nBody))
            .andExpect(status().isCreated())
            .andReturn();
        String neighborhoodId = json.readTree(nResult.getResponse().getContentAsString()).get("id").asText();

        // 4. Customer with cart
        UUID customerId = UUID.randomUUID();
        String addItem = """
            { "productId": "%s", "quantity": 1, "observation": "", "addOns": [] }
            """.formatted(productId);
        mvc.perform(asCustomer(post("/api/v1/cart/items"), customerId)
                .contentType(MediaType.APPLICATION_JSON).content(addItem))
            .andExpect(status().isCreated());

        // 5. Place order
        String placeBody = """
            {
              "modality": "DELIVERY",
              "address": {
                "street": "Rua T", "number": "1", "complement": null,
                "district": "X", "city": "Salvador", "postalCode": "40000-000",
                "neighborhoodId": "%s"
              }
            }
            """.formatted(neighborhoodId);
        MvcResult placeResult = mvc.perform(asCustomer(post("/api/v1/orders"), customerId)
                .header("Idempotency-Key", "key-" + suffix)
                .contentType(MediaType.APPLICATION_JSON).content(placeBody))
            .andExpect(status().isCreated())
            .andReturn();
        String orderId = json.readTree(placeResult.getResponse().getContentAsString()).get("id").asText();

        return new SetupIds(customerId, orderId);
    }

    private UsernamePasswordAuthenticationToken adminAuth() {
        return new UsernamePasswordAuthenticationToken(
            "admin-test", null,
            List.of(new SimpleGrantedAuthority("ROLE_OWNER")));
    }

    @Test
    void initiatePixHappyPath() throws Exception {
        SetupIds ids = setupOrder("pix-1");
        when(gateway.createPixCharge(any(), any(), any())).thenReturn(
            new PixCharge("MP-PIX-1", "qr-payload", "qr-base64", null));

        MvcResult result = mvc.perform(asCustomer(post("/api/v1/orders/" + ids.orderId() + "/payment"), ids.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"PIX\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.method").value("PIX"))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.qrCode").value("qr-payload"))
            .andReturn();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        String paymentId = body.get("id").asText();

        // Customer can fetch
        mvc.perform(asCustomer(get("/api/v1/payments/" + paymentId), ids.customerId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qrCodeBase64").value("qr-base64"));
    }

    @Test
    void initiatePaymentSecondTimeFailsAlreadyInitiated() throws Exception {
        SetupIds ids = setupOrder("pix-2");
        when(gateway.createPixCharge(any(), any(), any())).thenReturn(
            new PixCharge("MP-PIX-2", "qr", "qr64", null));

        mvc.perform(asCustomer(post("/api/v1/orders/" + ids.orderId() + "/payment"), ids.customerId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"PIX\"}"))
            .andExpect(status().isCreated());

        mvc.perform(asCustomer(post("/api/v1/orders/" + ids.orderId() + "/payment"), ids.customerId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"PIX\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void initiateCardMissingTokenFails() throws Exception {
        SetupIds ids = setupOrder("card-1");
        mvc.perform(asCustomer(post("/api/v1/orders/" + ids.orderId() + "/payment"), ids.customerId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"CARD\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void initiateCardApprovedAutoConfirmsOrder() throws Exception {
        SetupIds ids = setupOrder("card-ok");
        when(gateway.chargeCard(any(), any(), any(), any())).thenReturn(
            new CardCharge("MP-CARD-1", PaymentStatus.APPROVED, "1234", "VISA", "AUTH-1"));

        mvc.perform(asCustomer(post("/api/v1/orders/" + ids.orderId() + "/payment"), ids.customerId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"CARD\",\"cardToken\":\"tok-test\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        // AutoConfirmOrderOnApprovalListener fires AFTER_COMMIT — wait briefly for it.
        Thread.sleep(500);
        mvc.perform(asCustomer(get("/api/v1/orders/" + ids.orderId()), ids.customerId()))
            .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void webhookInvalidSignatureReturns200Silently() throws Exception {
        when(gateway.verifyWebhookSignature(any(), any())).thenReturn(false);

        mvc.perform(post("/api/v1/webhooks/mercado-pago")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"unknown-id\"}}"))
            .andExpect(status().isOk());
    }

    @Test
    void webhookForUnknownPaymentReturns200() throws Exception {
        mvc.perform(post("/api/v1/webhooks/mercado-pago")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"payment\",\"data\":{\"id\":\"never-seen\"}}"))
            .andExpect(status().isOk());
    }

    @Test
    void getPaymentForeignCustomerReturns404() throws Exception {
        SetupIds ids = setupOrder("foreign");
        when(gateway.createPixCharge(any(), any(), any())).thenReturn(
            new PixCharge("MP-FOREIGN", "qr", "qr64", null));

        MvcResult result = mvc.perform(asCustomer(post("/api/v1/orders/" + ids.orderId() + "/payment"), ids.customerId())
                .contentType(MediaType.APPLICATION_JSON).content("{\"method\":\"PIX\"}"))
            .andExpect(status().isCreated())
            .andReturn();
        String paymentId = json.readTree(result.getResponse().getContentAsString()).get("id").asText();

        mvc.perform(asCustomer(get("/api/v1/payments/" + paymentId), UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }
}
