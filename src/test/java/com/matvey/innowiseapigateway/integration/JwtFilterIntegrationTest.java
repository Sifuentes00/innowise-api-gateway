package com.matvey.innowiseapigateway.integration;

import com.matvey.innowiseapigateway.security.PublicKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class JwtFilterIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    private WebClient.Builder webClientBuilder;
    private PublicKeyProvider publicKeyProvider;

    @BeforeEach
    void setUp() {
        webClientBuilder = WebClient.builder();
        publicKeyProvider = new PublicKeyProvider(webClientBuilder);
    }

    @Test
    void postgresqlContainer_shouldStartSuccessfully() throws Exception {
        assertTrue(postgres.isRunning());
        assertNotNull(postgres.getJdbcUrl());
        assertNotNull(postgres.getUsername());
        assertNotNull(postgres.getPassword());

        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             Statement statement = connection.createStatement()) {
            
            statement.execute("CREATE TABLE jwt_test_table (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            statement.execute("INSERT INTO jwt_test_table (name) VALUES ('jwt-test')");
            
            var resultSet = statement.executeQuery("SELECT * FROM jwt_test_table");
            assertTrue(resultSet.next());
            assertEquals("jwt-test", resultSet.getString("name"));
        }
    }

    @Test
    void publicKeyProvider_shouldBeCreated() {
        assertNotNull(publicKeyProvider);
    }
}
