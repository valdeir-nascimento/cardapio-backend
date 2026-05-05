package com.cardapio;

import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class SmokeIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayAppliedMigrations() {
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = true",
            Integer.class);
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Test
    void eventPublicationTableExists() {
        Integer count = jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables " +
            "WHERE table_name = 'event_publication'",
            Integer.class);
        assertThat(count).isEqualTo(1);
    }
}
