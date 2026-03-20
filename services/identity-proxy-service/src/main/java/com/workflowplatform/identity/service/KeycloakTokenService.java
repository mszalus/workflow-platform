package com.workflowplatform.identity.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@Service
@Slf4j
public class KeycloakTokenService {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry = Instant.MIN;
    private final Object lock = new Object();
    private final RestTemplate restTemplate;

    public KeycloakTokenService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Returns a valid admin access token. If the cached token is still valid for
     * more than 30 seconds, it is returned directly. Otherwise, a new token is
     * fetched from Keycloak using client_credentials grant.
     */
    public String getAccessToken() {
        // Fast path: cached token valid for more than 30 seconds
        if (cachedToken != null && Instant.now().plusSeconds(30).isBefore(tokenExpiry)) {
            return cachedToken;
        }

        synchronized (lock) {
            // Re-check inside the lock to avoid duplicate fetches
            if (cachedToken != null && Instant.now().plusSeconds(30).isBefore(tokenExpiry)) {
                return cachedToken;
            }
            return fetchNewToken();
        }
    }

    private String fetchNewToken() {
        String tokenUrl = serverUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("access_token")) {
                throw new IllegalStateException("Keycloak token response missing access_token");
            }

            String token = (String) body.get("access_token");
            int expiresIn = parseExpiresIn(body);
            // Cache with expiry = now + (expires_in - 30) seconds, minimum 10 seconds
            long ttlSeconds = Math.max(expiresIn - 30L, 10L);
            cachedToken = token;
            tokenExpiry = Instant.now().plusSeconds(ttlSeconds);

            log.debug("Fetched new Keycloak admin token, expires in {}s (caching for {}s)", expiresIn, ttlSeconds);
            return token;

        } catch (RestClientException ex) {
            log.error("Failed to fetch Keycloak admin token: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Keycloak unavailable", ex);
        }
    }

    private int parseExpiresIn(Map<String, Object> body) {
        Object raw = body.get("expires_in");
        if (raw instanceof Number n) {
            return n.intValue();
        }
        // Fallback: treat as short-lived
        return 60;
    }
}
