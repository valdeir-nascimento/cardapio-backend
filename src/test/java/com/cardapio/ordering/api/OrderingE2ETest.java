package com.cardapio.ordering.api;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class OrderingE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private MockHttpServletRequestBuilder asCustomer(MockHttpServletRequestBuilder rb, UUID customerId) {
        var principal = new CardapioPrincipal(customerId, Audience.CUSTOMER, Set.of());
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        return rb.with(authentication(auth));
    }

    @Test
    @WithMockUser(roles = {"OWNER"})
    void fullFlowDeliveryIdempotencyAndStatusTransition() throws Exception {
        // 1. Setup: create category, product, neighborhood (admin context)
        String catBody = """
            { "name": "Pizzas E2E", "displayOrder": 1, "active": true }
            """;
        MvcResult catResult = mvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON).content(catBody))
            .andExpect(status().isCreated())
            .andReturn();
        String categoryId = json.readTree(catResult.getResponse().getContentAsString()).get("id").asText();

        String prodBody = """
            {
              "name": "Pizza Calabresa",
              "description": "Calabresa fatiada",
              "basePrice": 40.00,
              "categoryId": "%s",
              "imageUrl": null,
              "allowsHalfHalf": false,
              "variations": [],
              "addOnGroups": []
            }
            """.formatted(categoryId);
        MvcResult prodResult = mvc.perform(post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON).content(prodBody))
            .andExpect(status().isCreated())
            .andReturn();
        String productId = json.readTree(prodResult.getResponse().getContentAsString()).get("id").asText();

        String neighborhoodBody = """
            { "name": "Centro E2E", "city": "Salvador", "fee": 8.50, "active": true }
            """;
        MvcResult nResult = mvc.perform(post("/api/v1/admin/neighborhoods")
                .contentType(MediaType.APPLICATION_JSON).content(neighborhoodBody))
            .andExpect(status().isCreated())
            .andReturn();
        String neighborhoodId = json.readTree(nResult.getResponse().getContentAsString()).get("id").asText();

        // 2. Customer: add cart item
        UUID customerId = UUID.randomUUID();
        String addItemBody = """
            { "productId": "%s", "addOns": [], "observation": "sem azeitona", "quantity": 2 }
            """.formatted(productId);
        mvc.perform(asCustomer(post("/api/v1/cart/items"), customerId)
                .contentType(MediaType.APPLICATION_JSON).content(addItemBody))
            .andExpect(status().isCreated());

        // 3. Customer: get cart shows lineTotal
        mvc.perform(asCustomer(get("/api/v1/cart"), customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.subtotal").value(80.00));

        // 4. Customer: place order with idempotency key — DELIVERY
        String placeBody = """
            {
              "modality": "DELIVERY",
              "address": {
                "street": "Rua A", "number": "10", "complement": "ap 1",
                "district": "Centro", "city": "Salvador", "postalCode": "40000-000",
                "neighborhoodId": "%s"
              }
            }
            """.formatted(neighborhoodId);
        MvcResult placeResult = mvc.perform(asCustomer(post("/api/v1/orders"), customerId)
                .header("Idempotency-Key", "abc-123")
                .contentType(MediaType.APPLICATION_JSON).content(placeBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("RECEIVED"))
            .andExpect(jsonPath("$.total").value(88.50))   // 80 + 8.50
            .andReturn();
        JsonNode placedOrder = json.readTree(placeResult.getResponse().getContentAsString());
        String orderId = placedOrder.get("id").asText();

        // 5. Idempotency: second call with same key returns same id (no duplicate)
        MvcResult repeat = mvc.perform(asCustomer(post("/api/v1/orders"), customerId)
                .header("Idempotency-Key", "abc-123")
                .contentType(MediaType.APPLICATION_JSON).content(placeBody))
            .andExpect(status().isCreated())
            .andReturn();
        String repeatId = json.readTree(repeat.getResponse().getContentAsString()).get("id").asText();
        org.assertj.core.api.Assertions.assertThat(repeatId).isEqualTo(orderId);

        // 6. Customer: get own order
        mvc.perform(asCustomer(get("/api/v1/orders/" + orderId), customerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECEIVED"));

        // 7. Different customer should NOT see it (returns 404)
        mvc.perform(asCustomer(get("/api/v1/orders/" + orderId), UUID.randomUUID()))
            .andExpect(status().isNotFound());

        // 8. Admin lists orders, finds the new one
        mvc.perform(get("/api/v1/admin/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.id=='" + orderId + "')]").exists());

        // 9. Skipping CONFIRMED → invalid transition (422)
        String skipStatus = "{\"status\":\"PREPARING\"}";
        mvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON).content(skipStatus))
            .andExpect(status().isUnprocessableEntity());

        // 10. Walk through delivery happy path
        for (String s : new String[]{"CONFIRMED", "PREPARING", "READY", "OUT_FOR_DELIVERY", "DELIVERED"}) {
            mvc.perform(patch("/api/v1/admin/orders/" + orderId + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"status\":\"" + s + "\"}"))
                .andExpect(status().isNoContent());
        }

        // 11. Cancel after DELIVERED is invalid (422)
        mvc.perform(post("/api/v1/admin/orders/" + orderId + "/cancel"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = {"OWNER"})
    void publicDeliveryFeeReturns404ForUnknownNeighborhood() throws Exception {
        mvc.perform(get("/api/v1/delivery/fee").param("neighborhoodId", UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    void cartRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/cart"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"OWNER"})
    void placeOrderWithEmptyCartFails() throws Exception {
        UUID customerId = UUID.randomUUID();
        String placeBody = """
            { "modality": "PICKUP", "address": null }
            """;
        mvc.perform(asCustomer(post("/api/v1/orders"), customerId)
                .header("Idempotency-Key", "empty-cart-key")
                .contentType(MediaType.APPLICATION_JSON).content(placeBody))
            .andExpect(status().isUnprocessableEntity());
    }
}
