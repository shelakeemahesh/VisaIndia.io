package com.visasetu.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    private final DatabaseUrlEnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();

    @Test
    @DisplayName("Should translate Render postgres:// URL with credentials and port to Spring Boot JDBC properties")
    void shouldTranslateRenderPostgresUrlWithCredentialsAndPort() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("testSource", Map.of(
                "DATABASE_URL", "postgres://visasetu_user:secret_pass_123@dpg-c12345678-a.oregon-postgres.render.com:5432/visasetu_db"
        )));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://dpg-c12345678-a.oregon-postgres.render.com:5432/visasetu_db");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("visasetu_user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("secret_pass_123");
    }

    @Test
    @DisplayName("Should translate postgresql:// URL with query parameters and default port")
    void shouldTranslatePostgresqlUrlWithQueryAndDefaultPort() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("testSource", Map.of(
                "DATABASE_URL", "postgresql://admin:pass@postgres.internal/my_db?sslmode=require"
        )));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://postgres.internal:5432/my_db?sslmode=require");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("admin");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("pass");
    }

    @Test
    @DisplayName("Should fallback to SPRING_DATASOURCE_URL if DATABASE_URL is absent")
    void shouldFallbackToSpringDatasourceUrl() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("testSource", Map.of(
                "SPRING_DATASOURCE_URL", "postgres://fallback_user:fallback_pass@db.example.com:5433/prod_db"
        )));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://db.example.com:5433/prod_db");
        assertThat(environment.getProperty("spring.datasource.username"))
                .isEqualTo("fallback_user");
        assertThat(environment.getProperty("spring.datasource.password"))
                .isEqualTo("fallback_pass");
    }

    @Test
    @DisplayName("Should leave standard jdbc: URL unchanged without adding custom property source")
    void shouldLeaveStandardJdbcUrlUnchanged() {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("testSource", Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/local_db"
        )));

        processor.postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getPropertySources().contains(DatabaseUrlEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
        assertThat(environment.getProperty("SPRING_DATASOURCE_URL"))
                .isEqualTo("jdbc:postgresql://localhost:5432/local_db");
    }
}
