package com.innowise.apigateway.config;

import com.innowise.apigateway.security.TokenTypeValidator;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtConfigTest {

    private static final String KEY_ID = "auth-service-key-1";

    private MockWebServer jwksServer;
    private KeyPair keyPair;
    private ReactiveJwtDecoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        jwksServer = new MockWebServer();
        jwksServer.start();

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        jwksServer.enqueue(
                new MockResponse()
                        .setHeader("Content-Type", "application/json")
                        .setBody(createJwks())
        );

        String jwksUri = jwksServer.url("/.well-known/jwks.json").toString();

        decoder = new JwtConfig().jwtDecoder(
                jwksUri,
                new TokenTypeValidator()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        jwksServer.shutdown();
    }

    @Test
    void shouldDecodeValidAccessTokenUsingJwks() throws Exception {
        String token = createToken("access");

        Jwt jwt = decoder.decode(token).block();

        assertEquals("1002", jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertEquals(1002, (Long) jwt.getClaim("user_id"));
        assertEquals("access", jwt.getClaimAsString("token_type"));
    }

    @Test
    void shouldRejectRefreshToken() throws Exception {
        String token = createToken("refresh");

        assertThrows(
                JwtException.class,
                () -> decoder.decode(token).block()
        );
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() throws Exception {
        String token = createToken("access");

        String[] parts = token.split("\\.");

        String invalidSignature =
                (parts[2].startsWith("A") ? "B" : "A")
                        + parts[2].substring(1);

        String invalidToken =
                parts[0] + "." + parts[1] + "." + invalidSignature;

        assertThrows(
                JwtException.class,
                () -> decoder.decode(invalidToken).block()
        );
    }

    @Test
    void shouldRejectTokenWithoutTokenType() throws Exception {
        String token = createToken(null);

        assertThrows(
                JwtException.class,
                () -> decoder.decode(token).block()
        );
    }

    private String createToken(String tokenType) throws Exception {
        Instant now = Instant.now();

        String header = """
                {"alg":"RS256","kid":"auth-service-key-1","typ":"JWT"}
                """.trim();

        String tokenTypeClaim = tokenType == null
                ? ""
                : "\"token_type\": \"%s\",".formatted(tokenType);

        String payload = """
                {
                  "sub": "1002",
                  "user_id": 1002,
                  "role": "USER",
                  %s
                  "iat": %d,
                  "exp": %d
                }
                """.formatted(
                tokenTypeClaim,
                now.getEpochSecond(),
                now.plusSeconds(300).getEpochSecond()
        );

        String encodedHeader = encode(header);
        String encodedPayload = encode(payload);

        String signingInput = encodedHeader + "." + encodedPayload;

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());
        signature.update(signingInput.getBytes(StandardCharsets.UTF_8));

        String encodedSignature = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(signature.sign());

        return signingInput + "." + encodedSignature;
    }

    private String createJwks() {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

        String modulus = encodeInteger(publicKey.getModulus());
        String exponent = encodeInteger(publicKey.getPublicExponent());

        return """
                {
                  "keys": [
                    {
                      "kty": "RSA",
                      "use": "sig",
                      "alg": "RS256",
                      "kid": "%s",
                      "n": "%s",
                      "e": "%s"
                    }
                  ]
                }
                """.formatted(KEY_ID, modulus, exponent);
    }

    private String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encodeInteger(BigInteger value) {
        byte[] bytes = value.toByteArray();

        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
