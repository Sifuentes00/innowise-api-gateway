package com.matvey.innowiseapigateway.integration;

import com.matvey.innowiseapigateway.dto.AdminRegisterRequest;
import com.matvey.innowiseapigateway.dto.AuthResponse;
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
class RegistrationFlowIntegrationTest {

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
        mockServer = startClientAndServer(9999);
        webClientBuilder = WebClient.builder();
        
        String authServiceUrl = "http://localhost:9999";
        String userServiceUrl = "http://localhost:9999";
        
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
    void fullRegistrationFlow_shouldCompleteSuccessfully() {
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
                        .withStatusCode(200));

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/api/auth/login"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("{\"accessToken\":\"access-token-123\",\"refreshToken\":\"refresh-token-456\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(registrationService.register(request))
                .expectNextMatches(response -> 
                    response.getAccessToken().equals("access-token-123") &&
                    response.getRefreshToken().equals("refresh-token-456"))
                .verifyComplete();
    }

    @Test
    void adminRegistrationFlow_shouldCompleteSuccessfully() {
        AdminRegisterRequest request = AdminRegisterRequest.builder()
                .name("Admin")
                .surname("User")
                .email("admin@example.com")
                .password("adminpassword")
                .build();

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/internal/admin-credentials/.*"))
                .respond(response()
                        .withStatusCode(200));

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/internal/users"))
                .respond(response()
                        .withStatusCode(200));

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/api/auth/login"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("{\"accessToken\":\"admin-access-token\",\"refreshToken\":\"admin-refresh-token\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(registrationService.registerAdmin(request))
                .expectNextMatches(response -> 
                    response.getAccessToken().equals("admin-access-token") &&
                    response.getRefreshToken().equals("admin-refresh-token"))
                .verifyComplete();
    }

    @Test
    void loginFlow_shouldCompleteSuccessfully() {
        String email = "john@example.com";
        String password = "password";

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/api/auth/login"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("{\"accessToken\":\"login-access-token\",\"refreshToken\":\"login-refresh-token\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(authServiceClient.login(email, password))
                .expectNextMatches(response -> 
                    response.getAccessToken().equals("login-access-token") &&
                    response.getRefreshToken().equals("login-refresh-token"))
                .verifyComplete();
    }

    @Test
    void refreshFlow_shouldCompleteSuccessfully() {
        String refreshToken = "refresh-token-123";

        mockServer.when(request()
                .withMethod("POST")
                .withPath("/api/auth/refresh"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("{\"accessToken\":\"new-access-token\",\"refreshToken\":\"new-refresh-token\"}")
                        .withHeader("Content-Type", "application/json"));

        StepVerifier.create(authServiceClient.refresh(refreshToken))
                .expectNextMatches(response -> 
                    response.getAccessToken().equals("new-access-token") &&
                    response.getRefreshToken().equals("new-refresh-token"))
                .verifyComplete();
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
            
            statement.execute("CREATE TABLE test_table (id SERIAL PRIMARY KEY, name VARCHAR(100))");
            statement.execute("INSERT INTO test_table (name) VALUES ('test')");
            
            var resultSet = statement.executeQuery("SELECT * FROM test_table");
            assertTrue(resultSet.next());
            assertEquals("test", resultSet.getString("name"));
        }
    }
}
