package com.matvey.innowiseapigateway.controller;

import com.matvey.innowiseapigateway.dto.AdminRegisterRequest;
import com.matvey.innowiseapigateway.dto.AuthResponse;
import com.matvey.innowiseapigateway.dto.RegisterRequest;
import com.matvey.innowiseapigateway.dto.RoleType;
import com.matvey.innowiseapigateway.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    @Mock
    private RegistrationService registrationService;

    private RegistrationController controller;

    @BeforeEach
    void setUp() {
        controller = new RegistrationController(registrationService);
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

        when(registrationService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(controller.register(request))
                .expectNextMatches(entity -> 
                    entity.getStatusCode() == HttpStatus.CREATED &&
                    entity.getBody().getAccessToken().equals("access-token"))
                .verifyComplete();
    }

    @Test
    void registerAdmin_success() {
        AdminRegisterRequest request = AdminRegisterRequest.builder()
                .name("Admin")
                .surname("User")
                .email("admin@example.com")
                .password("adminpassword")
                .role(RoleType.ADMIN)
                .build();
        AuthResponse response = new AuthResponse("admin-access-token", "admin-refresh-token");

        when(registrationService.registerAdmin(any(AdminRegisterRequest.class)))
                .thenReturn(Mono.just(response));

        StepVerifier.create(controller.registerAdmin(request))
                .expectNextMatches(entity -> 
                    entity.getStatusCode() == HttpStatus.CREATED &&
                    entity.getBody().getAccessToken().equals("admin-access-token"))
                .verifyComplete();
    }

    @Test
    void register_failure_shouldPropagateError() {
        RegisterRequest request = RegisterRequest.builder()
                .name("John")
                .surname("Doe")
                .email("john@example.com")
                .password("password")
                .build();

        when(registrationService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("Registration failed")));

        StepVerifier.create(controller.register(request))
                .expectError()
                .verify();
    }
}
