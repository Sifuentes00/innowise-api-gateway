package com.matvey.innowiseapigateway.service;

import com.matvey.innowiseapigateway.dto.AdminRegisterRequest;
import com.matvey.innowiseapigateway.dto.AuthResponse;
import com.matvey.innowiseapigateway.dto.RegisterRequest;
import com.matvey.innowiseapigateway.dto.UserCreateRequest;
import com.matvey.innowiseapigateway.exception.AuthServiceException;
import com.matvey.innowiseapigateway.exception.RegistrationException;
import com.matvey.innowiseapigateway.exception.UserServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationService {

    private final AuthServiceClient authServiceClient;
    private final UserServiceClient userServiceClient;

    public Mono<AuthResponse> register(RegisterRequest request) {
        UUID userId = UUID.randomUUID();

        return authServiceClient.createCredentials(userId, request)
                .onErrorMap(error -> new AuthServiceException("Failed to create credentials in Auth Service: " + error.getMessage(), error))
                .flatMap(voidResult -> {
                    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
                            .userId(userId)
                            .name(request.getName())
                            .surname(request.getSurname())
                            .birthDate(request.getBirthDate())
                            .email(request.getEmail())
                            .build();

                    return userServiceClient.createUser(userCreateRequest);
                })
                .onErrorMap(error -> new UserServiceException("Failed to create user in User Service: " + error.getMessage(), error))
                .flatMap(voidResult -> authServiceClient.login(request.getEmail(), request.getPassword()))
                .onErrorMap(error -> new AuthServiceException("Failed to login after registration: " + error.getMessage(), error))
                .onErrorResume(error -> {
                    log.error("Registration failed for email: {}, rolling back", request.getEmail(), error);
                    return userServiceClient.deleteUser(userId)
                            .then(authServiceClient.deleteCredentials(userId))
                            .then(Mono.error(new RegistrationException("Registration failed for email: " + request.getEmail(), error)));
                });
    }

    public Mono<AuthResponse> registerAdmin(AdminRegisterRequest request) {
        UUID userId = UUID.randomUUID();

        return authServiceClient.createAdminCredentials(userId, request)
                .onErrorMap(error -> new AuthServiceException("Failed to create admin credentials in Auth Service: " + error.getMessage(), error))
                .flatMap(voidResult -> {
                    UserCreateRequest userCreateRequest = UserCreateRequest.builder()
                            .userId(userId)
                            .name(request.getName())
                            .surname(request.getSurname())
                            .birthDate(request.getBirthDate())
                            .email(request.getEmail())
                            .build();

                    return userServiceClient.createUser(userCreateRequest);
                })
                .onErrorMap(error -> new UserServiceException("Failed to create user in User Service: " + error.getMessage(), error))
                .flatMap(voidResult -> authServiceClient.login(request.getEmail(), request.getPassword()))
                .onErrorMap(error -> new AuthServiceException("Failed to login after registration: " + error.getMessage(), error))
                .onErrorResume(error -> {
                    log.error("Admin registration failed for email: {}, rolling back", request.getEmail(), error);
                    return userServiceClient.deleteUser(userId)
                            .then(authServiceClient.deleteCredentials(userId))
                            .then(Mono.error(new RegistrationException("Admin registration failed for email: " + request.getEmail(), error)));
                });
    }
}
