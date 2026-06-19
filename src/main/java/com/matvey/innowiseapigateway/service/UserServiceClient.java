package com.matvey.innowiseapigateway.service;

import com.matvey.innowiseapigateway.dto.UserCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.UUID;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${USER_SERVICE_URL}")
    private String userServiceUrl;

    public Mono<Void> createUser(UserCreateRequest request) {
        WebClient webClient = webClientBuilder.build();
        return webClient.post()
                .uri(userServiceUrl + "/internal/users")
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        status -> !status.is2xxSuccessful(),
                        response -> response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Failed to create user: " + body)))
                )
                .bodyToMono(Void.class);
    }
}
