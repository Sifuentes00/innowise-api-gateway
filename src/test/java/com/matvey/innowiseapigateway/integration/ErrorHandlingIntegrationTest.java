package com.matvey.innowiseapigateway.integration;

import com.matvey.innowiseapigateway.dto.RegisterRequest;
import com.matvey.innowiseapigateway.service.AuthServiceClient;
import com.matvey.innowiseapigateway.service.RegistrationService;
import com.matvey.innowiseapigateway.service.UserServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@Testcontainers
class ErrorHandlingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    private ClientAndServer mockServer;
    private WebClient.Builder webClientBuilder;
    private RegistrationService registrationService;
    private AuthServiceClient authServiceClient;
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        mockServer = startClientAndServer(9998);
        webClientBuilder = WebClient.builder();
        
        String authServiceUrl = "http://localhost:9998";
        String userServiceUrl = "http://localhost:9998";
        
        authServiceClient = new AuthServiceClient(webClientBuilder);
        userServiceClient = new UserServiceClient(webClientBuilder);
        
        setField(authServiceClient, "authServiceUrl", authServiceUrl);
        setField(userServiceClient, "userServiceUrl", userServiceUrl);
        
        registrationService = new RegistrationService(authServiceClient, userServiceClient);
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    private void setField(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void authServiceFailure_shouldHandleErrorGracefully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password")
                .build();

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/internal/credentials/.*"))
                .respond(response()
                        .withStatusCode(500)
                        .withBody("{\"error\":\"Internal server error\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(registrationService.register(request))
                .expectError()
                .verify();
    }

    @Test
    void userServiceFailure_shouldHandleErrorGracefully() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password")
                .build();

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/internal/credentials/.*"))
                .respond(response()
                        .withStatusCode(200));

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/internal/users"))
                .respond(response()
                        .withStatusCode(500)
                        .withBody("{\"error\":\"Internal server error\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(registrationService.register(request))
                .expectError()
                .verify();
    }

    @Test
    void loginFailure_shouldHandleErrorGracefully() {
        String email = "john@example.com";
        String password = "wrongpassword";

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/api/auth/login"))
                .respond(response()
                        .withStatusCode(401)
                        .withBody("{\"error\":\"Invalid credentials\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(authServiceClient.login(email, password))
                .expectError()
                .verify();
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
            
            statement.execute("CREATE TABLE error_test_table (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            statement.execute("INSERT INTO error_test_table (name) VALUES ('error-test')");
            
            var resultSet = statement.executeQuery("SELECT * FROM error_test_table");
            assertTrue(resultSet.next());
            assertEquals("error-test", resultSet.getString("name"));
        }
    }
}
