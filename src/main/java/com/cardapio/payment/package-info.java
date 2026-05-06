@org.springframework.modulith.ApplicationModule(
    displayName = "Payment",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "ordering::OrderingFacade",
        "ordering::dto",
        "ordering::ids",
        "ordering::commands",
        "identity::security",
        "identity::ids"
    }
)
package com.cardapio.payment;
