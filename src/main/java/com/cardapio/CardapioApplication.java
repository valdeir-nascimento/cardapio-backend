package com.cardapio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.modulith.Modulithic;

import java.time.Clock;

@SpringBootApplication
@ConfigurationPropertiesScan
@Modulithic(systemName = "Cardapio Digital")
public class CardapioApplication {

    public static void main(String[] args) {
        SpringApplication.run(CardapioApplication.class, args);
    }

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
