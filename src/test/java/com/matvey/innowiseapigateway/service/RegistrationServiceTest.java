package com.matvey.innowiseapigateway.service;

import com.matvey.innowiseapigateway.dto.AdminRegisterRequest;
import com.matvey.innowiseapigateway.dto.AuthResponse;
import com.matvey.innowiseapigateway.dto.RegisterRequest;
import com.matvey.innowiseapigateway.dto.UserCreateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AuthServiceClient authServiceClient;

    @Mock
    private UserServiceClient userServiceClient;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(authServiceClient, userServiceClient);
    }

    @Test
    void register_success() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password")
                .build();
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authServiceClient.createCredentials(any(UUID.class), any(RegisterRequest.class)))
                .thenReturn(Mono.empty());
        when(userServiceClient.createUser(any(UserCreateRequest.class)))
                .thenReturn(Mono.empty());
        when(authServiceClient.login(anyString(), anyString()))
                .thenAnswer(invocation -> Mono.just(response));

        StepVerifier.create(registrationService.register(request))
                .expectNextMatches(r -> r.getAccessToken().equals("access-token") && r.getRefreshToken().equals("refresh-token"))
                .verifyComplete();

        verify(authServiceClient).createCredentials(any(UUID.class), any(RegisterRequest.class));
        verify(userServiceClient).createUser(any(UserCreateRequest.class));
        verify(authServiceClient).login(anyString(), anyString());
    }

    @Test
    void registerAdmin_success() {
        AdminRegisterRequest request = AdminRegisterRequest.builder()
                .name("Admin")
                .surname("User")
                .email("admin@example.com")
                .password("adminpassword")
                .build();
        AuthResponse response = new AuthResponse("admin-access-token", "admin-refresh-token");

        when(authServiceClient.createAdminCredentials(any(UUID.class), any(AdminRegisterRequest.class)))
                .thenReturn(Mono.empty());
        when(userServiceClient.createUser(any(UserCreateRequest.class)))
                .thenReturn(Mono.empty());
        when(authServiceClient.login(anyString(), anyString()))
                .thenAnswer(invocation -> Mono.just(response));

        StepVerifier.create(registrationService.registerAdmin(request))
                .expectNextMatches(r -> r.getAccessToken().equals("admin-access-token") && r.getRefreshToken().equals("admin-refresh-token"))
                .verifyComplete();

        verify(authServiceClient).createAdminCredentials(any(UUID.class), any(AdminRegisterRequest.class));
        verify(userServiceClient).createUser(any(UserCreateRequest.class));
        verify(authServiceClient).login(anyString(), anyString());
    }

    @Test
    void register_authServiceFails_shouldThrowException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password")
                .build();

        when(authServiceClient.createCredentials(any(UUID.class), any(RegisterRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Auth service error")));

        StepVerifier.create(registrationService.register(request))
                .expectError()
                .verify();

        verify(userServiceClient, never()).createUser(any(UserCreateRequest.class));
    }

    @Test
    void register_userServiceFails_shouldThrowException() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password")
                .build();

        when(authServiceClient.createCredentials(any(UUID.class), any(RegisterRequest.class)))
                .thenReturn(Mono.empty());
        when(userServiceClient.createUser(any(UserCreateRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("User service error")));

        StepVerifier.create(registrationService.register(request))
                .expectError()
                .verify();

        verify(authServiceClient).createCredentials(any(UUID.class), any(RegisterRequest.class));
        verify(userServiceClient).createUser(any(UserCreateRequest.class));
    }
}
