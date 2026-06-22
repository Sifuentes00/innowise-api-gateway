package com.matvey.innowiseapigateway.filter;

import com.matvey.innowiseapigateway.security.PublicKeyProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private PublicKeyProvider publicKeyProvider;

    @Mock
    private WebFilterChain chain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(publicKeyProvider);
    }

    @Test
    void whenPublicPath_thenShouldSkipValidation() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(publicKeyProvider, never()).validateToken(anyString());
    }

    @Test
    void whenRegisterPath_thenShouldSkipValidation() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/register").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(publicKeyProvider, never()).validateToken(anyString());
    }

    @Test
    void whenAdminRegisterPath_thenShouldSkipValidation() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/auth/admin/register").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(publicKeyProvider, never()).validateToken(anyString());
    }

    @Test
    void whenPublicKeyPath_thenShouldSkipValidation() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/public-key").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        verify(publicKeyProvider, never()).validateToken(anyString());
    }

    @Test
    void whenProtectedPathWithoutToken_thenShouldReturnUnauthorized() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/profile").build()
        );

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void whenProtectedPathWithInvalidToken_thenShouldReturnUnauthorized() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/profile")
                        .header("Authorization", "Bearer invalid.token")
                        .build()
        );

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void whenPathStartsWithPublicPath_thenShouldSkipValidation() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/login/something").build()
        );

        when(chain.filter(exchange)).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
    }
}
