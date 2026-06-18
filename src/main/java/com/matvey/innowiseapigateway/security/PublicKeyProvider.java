package com.matvey.innowiseapigateway.security;

import com.matvey.innowiseapigateway.dto.PublicKeyResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class PublicKeyProvider {

    private final WebClient.Builder webClientBuilder;
    private PublicKey publicKey;

    @Value("${AUTH_SERVICE_URL}")
    private String authServiceUrl;

    public PublicKeyProvider(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public PublicKey getPublicKey() {
        if (publicKey == null) {
            try {
                WebClient webClient = webClientBuilder.build();
                PublicKeyResponse response = webClient.get()
                        .uri(authServiceUrl + "/api/auth/public-key")
                        .retrieve()
                        .bodyToMono(PublicKeyResponse.class)
                        .block();

                publicKey = parsePublicKey(response.getPublicKey());
            } catch (Exception e) {
                log.error("Failed to fetch public key from Auth Service", e);
                throw new RuntimeException("Failed to fetch public key", e);
            }
        }
        return publicKey;
    }

    public boolean validateToken(String token) {
        try {
            PublicKey publicKey = getPublicKey();
            JwtParser parser = Jwts.parser()
                    .verifyWith(publicKey)
                    .build();

            Claims claims = parser.parseSignedClaims(token).getPayload();
            return !claims.getExpiration().before(new Date());

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return false;
        }
    }

    public UUID extractUserId(String token) {
        PublicKey publicKey = getPublicKey();
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    public String extractRole(String token) {
        PublicKey publicKey = getPublicKey();
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    public String extractTokenType(String token) {
        PublicKey publicKey = getPublicKey();
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("token-type", String.class);
    }

    private PublicKey parsePublicKey(String pem) throws Exception {
        String publicKeyPEM = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
