package com.cardapio.shared.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI cardapioOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Cardápio Digital — API")
                .version("v1")
                .description("""
                    Backend para cardápio digital com múltiplos fluxos:

                    - **Identidade** — cadastro/login de clientes e admins, OAuth Google/Apple, refresh de token.
                    - **Catálogo** — categorias, produtos, horários de funcionamento.
                    - **Pedidos** — carrinho, checkout, comandas, mesas (QR).
                    - **Pagamentos** — Pix e cartão via Mercado Pago, webhook de notificação.
                    - **Promoções, Reviews, Entregas, Notificações.**

                    ### Autenticação

                    A maior parte das chamadas exige um JWT no header `Authorization: Bearer <token>`,
                    obtido em `/api/v1/auth/login` (cliente) ou `/api/v1/admin/auth/login` (admin).

                    ### Convenções

                    - Todas as datas são ISO-8601 em UTC (`2026-05-07T20:30:00Z`).
                    - Valores monetários são `BigDecimal` em **reais** com 2 casas (`12.50`).
                    - IDs públicos são UUID v4.
                    - Telefones são E.164 (`+5511999998888`).
                    - CEP no formato `00000-000`.
                    - Erros seguem o padrão `{ "code": "ERROR_CODE", "message": "...", "details": {...} }`.
                    """)
                .contact(new Contact()
                    .name("Cardápio Backend Team")
                    .email("dev@cardapio.local"))
                .license(new License().name("Proprietary")))
            .servers(List.of(
                new Server().url("http://localhost:8080").description("Local"),
                new Server().url("https://api.cardapio.example.com").description("Produção")
            ))
            .components(new Components()
                .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT obtido em `/api/v1/auth/login` ou `/api/v1/admin/auth/login`.")))
            .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
