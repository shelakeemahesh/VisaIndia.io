package com.visasetu.schema;

import com.visasetu.domain.entity.Trip;
import com.visasetu.domain.entity.User;
import com.visasetu.repository.TripRepository;
import com.visasetu.repository.UserRepository;
import com.visasetu.repository.VisaCategoryDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true",
                "spring.flyway.baseline-on-migrate=true",
                "visasetu.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "visasetu.security.pii-encryption-key=dGhpcy1pcy1hLTMyLWJ5dGUtc2VjcmV0LWtleS0xMjM=",
                "visasetu.razorpay.key-id=rzp_test_mock_123",
                "visasetu.razorpay.key-secret=mock_secret_456",
                "visasetu.razorpay.webhook-secret=mock_webhook_secret_789"
        }
)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SchemaValidationPostgresTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private VisaCategoryDetailRepository visaCategoryDetailRepository;

    @Test
    @DisplayName("Flyway migrations apply and Hibernate ddl-auto=validate passes against PostgreSQL 16")
    void flywayAndHibernateValidation_passesOnPostgres16() {
        assertThat(visaCategoryDetailRepository.count()).isGreaterThan(0);

        User user = User.builder()
                .email("test.pg@example.com")
                .passwordHash("$2a$10$abcdefghijklmnopqrstuv")
                .fullName("Postgres Test User")
                .role("ROLE_TRAVELER")
                .emailVerified(true)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();

        Trip trip = Trip.builder()
                .referenceId("VS-TEST-PG-" + UUID.randomUUID().toString().substring(0, 8))
                .userId(savedUser.getId())
                .applicantEmail("test.pg@example.com")
                .applicantName("Postgres Test User")
                .nationality("United States")
                .categoryName("e-Tourist Visa (30 Days)")
                .status("DRAFT")
                .currency("INR")
                .totalAmount(new BigDecimal("25.00"))
                .arrivalDate(LocalDate.now().plusDays(10))
                .portOfArrival("DEL")
                .build();
        Trip savedTrip = tripRepository.save(trip);
        assertThat(savedTrip.getId()).isNotNull();
        assertThat(savedTrip.getApplicantName()).isEqualTo("Postgres Test User");
    }
}
