package com.cardapio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@Modulithic(systemName = "Cardapio Digital")
@SpringBootApplication
public class CardapioApplication {
    public static void main(String[] args) {
        SpringApplication.run(CardapioApplication.class, args);
    }
}
