@org.springframework.modulith.ApplicationModule(
    displayName = "Promotion",
    allowedDependencies = {
        "shared",
        "api::error",
        "api::support",
        "ordering::events",
        "ordering::ids",
        "ordering::ports",
        "ordering::dto"
    }
)
package com.cardapio.promotion;
