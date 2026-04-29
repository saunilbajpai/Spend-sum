package com.Spendsum.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Shared Testcontainers configuration used by all integration and repository tests.
 *
 * Using a STATIC container means a single MySQL instance is reused across ALL tests
 * in the same JVM invocation — this is far faster than starting fresh per test class.
 *
 * Extend this class (or annotate with @Import) in any @SpringBootTest that needs a
 * real database.
 */
@TestConfiguration
@Testcontainers
public class TestcontainersConfig {

    /**
     * A single shared MySQL 8 container.
     * @Container + static field = Testcontainers manages lifecycle automatically.
     */
    @Container
    public static final MySQLContainer<?> mysqlContainer =
            new MySQLContainer<>("mysql:8.0")
                    .withDatabaseName("spendsum_test")
                    .withUsername("test")
                    .withPassword("test")
                    .withReuse(true); // reuse between test classes for speed

    static {
        // Start eagerly so @DynamicPropertySource gets the live port
        mysqlContainer.start();
    }

    /**
     * Dynamically injects the real JDBC URL from the running container into
     * Spring's property environment — overriding application-test.properties.
     */
    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }
}
