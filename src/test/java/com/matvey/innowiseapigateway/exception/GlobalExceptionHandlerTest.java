package com.matvey.innowiseapigateway.exception;

import com.matvey.innowiseapigateway.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private BindingResult bindingResult;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/test").build());
    }

    @Test
    void handleRegistrationException_shouldReturnBadRequest() {
        RegistrationException ex = new RegistrationException("Registration failed");

        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleRegistrationException(ex, exchange);

        StepVerifier.create(result)
                .expectNextMatches(response -> 
                    response.getStatusCode() == HttpStatus.BAD_REQUEST &&
                    response.getBody().getError().equals("Registration Failed"))
                .verifyComplete();
    }

    @Test
    void handleAuthServiceException_shouldReturnBadGateway() {
        AuthServiceException ex = new AuthServiceException("Auth service error");

        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleAuthServiceException(ex, exchange);

        StepVerifier.create(result)
                .expectNextMatches(response -> 
                    response.getStatusCode() == HttpStatus.BAD_GATEWAY &&
                    response.getBody().getError().equals("Auth Service Error"))
                .verifyComplete();
    }

    @Test
    void handleUserServiceException_shouldReturnBadGateway() {
        UserServiceException ex = new UserServiceException("User service error");

        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleUserServiceException(ex, exchange);

        StepVerifier.create(result)
                .expectNextMatches(response -> 
                    response.getStatusCode() == HttpStatus.BAD_GATEWAY &&
                    response.getBody().getError().equals("User Service Error"))
                .verifyComplete();
    }

    @Test
    void handleValidationException_shouldReturnBadRequest() {
        FieldError fieldError = new FieldError("object", "email", "invalid", false, null, null, "Email is invalid");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleValidationException(ex, exchange);

        StepVerifier.create(result)
                .expectNextMatches(response -> 
                    response.getStatusCode() == HttpStatus.BAD_REQUEST &&
                    response.getBody().getError().equals("Validation Failed") &&
                    response.getBody().getMessage().equals("Email is invalid"))
                .verifyComplete();
    }

    @Test
    void handleGenericException_shouldReturnInternalServerError() {
        Exception ex = new Exception("Unexpected error");

        Mono<ResponseEntity<ErrorResponse>> result = exceptionHandler.handleGenericException(ex, exchange);

        StepVerifier.create(result)
                .expectNextMatches(response -> 
                    response.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR &&
                    response.getBody().getError().equals("Internal Server Error"))
                .verifyComplete();
    }
}
