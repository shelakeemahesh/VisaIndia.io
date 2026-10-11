package com.visasetu.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Automatically parses cloud database connection strings (e.g. Render, Railway, Fly.io)
 * from DATABASE_URL or SPRING_DATASOURCE_URL in the format:
 * postgres[ql]://[user:password@]host[:port]/database[?params]
 * and registers compliant Spring Boot JDBC properties.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    public static final String PROPERTY_SOURCE_NAME = "cloudDatabaseUrlProperties";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = environment.getProperty("SPRING_DATASOURCE_URL");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }

        String rawUrl = dbUrl.trim();
        if (rawUrl.startsWith("postgres://") || rawUrl.startsWith("postgresql://")) {
            try {
                // Normalize scheme for java.net.URI parsing
                String normalized = rawUrl.startsWith("postgres://")
                        ? "http://" + rawUrl.substring("postgres://".length())
                        : "http://" + rawUrl.substring("postgresql://".length());

                URI uri = new URI(normalized);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath(); // e.g. /visasetu_db
                String query = uri.getQuery();

                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + (path != null ? path : "/visasetu_db")
                        + (query != null && !query.isBlank() ? "?" + query : "");

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);

                String userInfo = uri.getUserInfo();
                if (userInfo != null && !userInfo.isBlank()) {
                    if (userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        props.put("spring.datasource.username", parts[0]);
                        props.put("spring.datasource.password", parts[1]);
                    } else {
                        props.put("spring.datasource.username", userInfo);
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, props));
            } catch (Exception e) {
                // If URI parsing fails, retain existing Spring environment properties
            }
        }
    }
}
