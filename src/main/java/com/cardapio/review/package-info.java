@org.springframework.modulith.ApplicationModule(
    displayName = "Review",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "ordering::events",
        "ordering::ids",
        "ordering::OrderingFacade",
        "ordering::dto",
        "identity::IdentityFacade",
        "identity::dto",
        "identity::security"
    }
)
package com.cardapio.review;
