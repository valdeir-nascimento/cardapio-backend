@org.springframework.modulith.ApplicationModule(
    displayName = "Notification",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "ordering::OrderingFacade",
        "ordering::dto",
        "ordering::ids",
        "ordering::events",
        "identity::IdentityFacade",
        "identity::dto",
        "identity::ids",
        "identity::security",
        "payment::events",
        "payment::ids"
    }
)
package com.cardapio.notification;
