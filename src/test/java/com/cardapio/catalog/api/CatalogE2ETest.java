package com.cardapio.catalog.api;

import com.cardapio.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class CatalogE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    @WithMockUser(roles = {"OWNER"})
    void adminCreatesCategoryAndProduct_publicSeesMenu() throws Exception {
        // 1. Create category
        String catBody = """
            { "name": "Pizzas", "displayOrder": 1, "active": true }
            """;
        MvcResult catResult = mvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON).content(catBody))
            .andExpect(status().isCreated())
            .andReturn();
        JsonNode cat = json.readTree(catResult.getResponse().getContentAsString());
        String categoryId = cat.get("id").asText();

        // 2. Create product
        String prodBody = """
            {
              "name": "Pizza Margherita",
              "description": "Molho, mussarela, manjericão",
              "basePrice": 39.90,
              "categoryId": "%s",
              "imageUrl": null,
              "allowsHalfHalf": true,
              "variations": [
                {"name": "Pequena", "priceModifier": 0.00},
                {"name": "Grande", "priceModifier": 10.00}
              ],
              "addOnGroups": [
                {"name": "Adicionais", "minSelection": 0, "maxSelection": 3,
                 "items": [{"name": "Bacon", "price": 3.00}]}
              ]
            }
            """.formatted(categoryId);
        mvc.perform(post("/api/v1/admin/products")
                .contentType(MediaType.APPLICATION_JSON).content(prodBody))
            .andExpect(status().isCreated());
    }

    @Test
    void publicMenuIsAnonymous() throws Exception {
        mvc.perform(get("/api/v1/menu"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void publicOperatingHoursIsAnonymous() throws Exception {
        mvc.perform(get("/api/v1/operating-hours"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.hoursByDay").exists());
    }

    @Test
    void adminEndpointRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/admin/categories"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"OPERATOR"})
    void operatorCannotManageCategories() throws Exception {
        mvc.perform(post("/api/v1/admin/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"X\",\"displayOrder\":1,\"active\":true}"))
            .andExpect(status().isForbidden());
    }
}
