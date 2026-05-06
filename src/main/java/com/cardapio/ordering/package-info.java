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
        "identity::ids",
        "promotion::CouponQueryPort",
        "promotion::ids",
        "promotion::dto",
        "promotion::evaluation"
    }
)
package com.cardapio.ordering;
