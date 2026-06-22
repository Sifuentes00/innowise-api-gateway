package com.matvey.innowiseapigateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PublicKeyProviderTest {

    @Mock
    private org.springframework.web.reactive.function.client.WebClient.Builder webClientBuilder;

    private PublicKeyProvider publicKeyProvider;

    @BeforeEach
    void setUp() {
        publicKeyProvider = new PublicKeyProvider(webClientBuilder);
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        String invalidToken = "invalid.token.here";

        boolean result = publicKeyProvider.validateToken(invalidToken);

        assertFalse(result);
    }

    @Test
    void extractUserId_withInvalidToken_shouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> publicKeyProvider.extractUserId(invalidToken));
    }

    @Test
    void extractRole_withInvalidToken_shouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> publicKeyProvider.extractRole(invalidToken));
    }

    @Test
    void extractTokenType_withInvalidToken_shouldThrowException() {
        String invalidToken = "invalid.token.here";

        assertThrows(Exception.class, () -> publicKeyProvider.extractTokenType(invalidToken));
    }
}
