package com.matvey.innowiseapigateway.service;

import com.matvey.innowiseapigateway.dto.AdminRegisterRequest;
import com.matvey.innowiseapigateway.dto.AuthResponse;
import com.matvey.innowiseapigateway.dto.LoginRequest;
import com.matvey.innowiseapigateway.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${AUTH_SERVICE_URL}")
    private String authServiceUrl;

    public Mono<Void> createCredentials(UUID userId, RegisterRequest request) {
        WebClient webClient = webClientBuilder.build();
        return webClient.post()
                .uri(authServiceUrl + "/internal/credentials/" + userId)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Failed to create credentials: " + body)))
                )
                .bodyToMono(Void.class);
    }

    public Mono<Void> createAdminCredentials(UUID userId, AdminRegisterRequest request) {
        WebClient webClient = webClientBuilder.build();
        return webClient.post()
                .uri(authServiceUrl + "/internal/admin-credentials/" + userId)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Failed to create admin credentials: " + body)))
                )
                .bodyToMono(Void.class);
    }

    public Mono<Void> deleteCredentials(UUID userId) {
        WebClient webClient = webClientBuilder.build();
        return webClient.delete()
                .uri(authServiceUrl + "/internal/credentials/" + userId)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Failed to delete credentials: " + body)))
                )
                .bodyToMono(Void.class);
    }

    public Mono<AuthResponse> login(String email, String password) {
        WebClient webClient = webClientBuilder.build();
        LoginRequest loginRequest = new LoginRequest(email, password);

        return webClient.post()
                .uri(authServiceUrl + "/api/auth/login")
                .bodyValue(loginRequest)
                .retrieve()
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Failed to login: " + body)))
                )
                .bodyToMono(AuthResponse.class);
    }
}
