@org.springframework.modulith.ApplicationModule(
    displayName = "Promotion",
    allowedDependencies = {"shared", "api::error", "api::support", "ordering::events", "ordering::ids"}
)
package com.cardapio.promotion;
