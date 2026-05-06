@org.springframework.modulith.ApplicationModule(
    displayName = "Ordering",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "catalog::CatalogFacade",
        "catalog::dto",
        "catalog::ids",
        "delivery::DeliveryFacade",
        "delivery::ids",
        "identity::security",
        "identity::ids"
    }
)
package com.cardapio.ordering;
