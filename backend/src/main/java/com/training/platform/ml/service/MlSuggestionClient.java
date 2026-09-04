package com.training.platform.ml.service;

import com.training.platform.ml.config.MlProperties;
import com.training.platform.ml.dto.MlSuggestRequest;
import com.training.platform.ml.dto.MlSuggestResponse;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class MlSuggestionClient {

    private static final Logger log = LoggerFactory.getLogger(MlSuggestionClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final MlProperties properties;

    public MlSuggestionClient(
            @Qualifier("mlRestTemplate") RestTemplate restTemplate,
            MlProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public Optional<MlSuggestResponse> suggest(MlSuggestRequest request) {
        if (!properties.isEnabled()) {
            return Optional.empty();
        }
        try {
            String url = url("/suggest");
            ResponseEntity<MlSuggestResponse> response = restTemplate.postForEntity(
                    url,
                    request,
                    MlSuggestResponse.class
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("MLA service returned empty/non-success response: {}", response.getStatusCode());
                return Optional.empty();
            }
            return Optional.of(response.getBody());
        } catch (RestClientException ex) {
            log.warn("MLA service unavailable, falling back to rule-based suggestions: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public Map<String, Object> getPipeline() {
        return getMap("/pipeline");
    }

    public Map<String, Object> getMetrics() {
        return getMap("/metrics");
    }

    public Map<String, Object> getHealth() {
        return getMap("/health");
    }

    public Map<String, Object> getDatasetSample(int limit) {
        return getMap("/dataset/sample?limit=" + Math.max(1, Math.min(limit, 50)));
    }

    public Map<String, Object> retrain() {
        try {
            RestTemplate longTimeout = longTimeoutTemplate();
            ResponseEntity<Map<String, Object>> response = longTimeout.exchange(
                    url("/retrain"),
                    HttpMethod.POST,
                    null,
                    MAP_TYPE
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("MLA retrain failed with status " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new IllegalStateException("MLA service unavailable: " + ex.getMessage(), ex);
        }
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private Map<String, Object> getMap(String path) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url(path),
                    HttpMethod.GET,
                    null,
                    MAP_TYPE
            );
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("MLA endpoint " + path + " failed: " + response.getStatusCode());
            }
            return response.getBody();
        } catch (RestClientException ex) {
            throw new IllegalStateException("MLA service unavailable: " + ex.getMessage(), ex);
        }
    }

    private RestTemplate longTimeoutTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(180_000);
        return new RestTemplate(factory);
    }

    private String url(String path) {
        String base = trimSlash(properties.getBaseUrl());
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    private String trimSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://127.0.0.1:8000";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
