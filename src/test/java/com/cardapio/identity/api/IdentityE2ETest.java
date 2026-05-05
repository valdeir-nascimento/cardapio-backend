package com.cardapio.identity.api;

import com.cardapio.support.PostgresTestContainerConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class IdentityE2ETest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void fullRegisterLoginRefreshFlow() throws Exception {
        // 1. Register
        String registerBody = """
            {
              "name": "Maria Silva",
              "email": "maria-e2e@example.com",
              "phoneNumber": "+5511912345678",
              "password": "S3curePass!"
            }
            """;
        mvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());

        // 2. Login
        String loginBody = """
            { "email": "maria-e2e@example.com", "password": "S3curePass!" }
            """;
        MvcResult loginResult = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists())
            .andReturn();

        JsonNode loginJson = json.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginJson.get("accessToken").asText();
        String refreshToken = loginJson.get("refreshToken").asText();

        // 3. Use access token on /me
        mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("maria-e2e@example.com"))
            .andExpect(jsonPath("$.name").value("Maria Silva"));

        // 4. Refresh token
        String refreshBody = "{ \"refreshToken\": \"" + refreshToken + "\" }";
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(jsonPath("$.refreshToken").exists());

        // 5. Old refresh should be revoked now (rotation)
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON).content(refreshBody))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsLoginWithWrongPassword() throws Exception {
        // pre-register a user
        String reg = """
            {"name":"X","email":"wrongpass@example.com","phoneNumber":"+5511912345678","password":"S3curePass!"}
            """;
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(reg))
            .andExpect(status().isCreated());

        String wrong = """
            {"email":"wrongpass@example.com","password":"WrongPass!1"}
            """;
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON).content(wrong))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void meRequiresAuth() throws Exception {
        mvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized());
    }
}
