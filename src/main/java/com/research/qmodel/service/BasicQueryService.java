package com.research.qmodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Service
public class BasicQueryService {
    final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Logger LOGGER = LoggerFactory.getLogger(BasicQueryService.class);
    @Value("${app.mode}")
    private String mode;
    @Value("${qmodel.api.key}")
    private String[] apiKey;
    @Value("${qmodel.api.key}")
    private String[] apiKeyBackup;
    private String currentKey;
    @Value("${app.demo_batch_limit}")
    private int DEMO_BATCH_LIMIT;
    @Value("${app.prod_batch_limit}")
    private int PROD_BATCH_LIMIT;
    @Value("${app.page_size}")
    private int PAGE_SIZE;
    @Value("${app.base_url}")
    private String BASE_URL;

    public BasicQueryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T retrievemetrics(String url, TypeReference<T> targetType) {
        List<T> allEntities = new ArrayList<>();
        int pageNumber = 70;
        while (isRun(pageNumber)) {
            JsonNode body = getRowData(url, BASE_URL, pageNumber, PAGE_SIZE);
            if (body != null && body.size() > 0) {
                Class<T> targetTypeClass = (Class<T>) targetType.getType();
                T t = objectMapper.convertValue(body, targetTypeClass);
                boolean isAdded = t instanceof List ? ((List<T>) t).addAll(allEntities) : allEntities.add(t);
                if (isAdded) {
                    if (url.contains("actions/run")) {
                        return t;
                    }
                    LOGGER.info("getting page number {}", pageNumber);
                    pageNumber++;
                }
            } else {
                break;
            }
        }
        return objectMapper.convertValue(allEntities, targetType);
    }

    private boolean isRun(int pageNumber) {
        return mode.equals("demo") ? pageNumber < 150 : true;
    }

    private JsonNode getRowData(String url, String baseUrl, int pageNumber, int pageSize) {
        String apiUrl = String.format(url, baseUrl, "?page=" + pageNumber + "&per_page=" + pageSize);
        HttpHeaders headers = new HttpHeaders();
        currentKey = currentKey == null ? apiKey[0] : currentKey;
        headers.set("Authorization", currentKey);
        RequestEntity<Void> requestEntity;
        try {
            requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(apiUrl));
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
            JsonNode body = response.getBody();
            return body;
        } catch (Exception e) {
            currentKey = apiKey[1];
            headers.set("Authorization", currentKey);
            try {
                requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(apiUrl));
                ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
                JsonNode body = response.getBody();
                return body;
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public JsonNode getRowData(String commitUrl) {
        RequestEntity<Void> requestEntity = setUpCallMeta(commitUrl);
        if (requestEntity == null) return null;
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            if (e.getMessage().toLowerCase().contains("forbidden")) {
                if (this.apiKey != null && this.apiKey.length != 0) {
                    this.apiKey = Arrays.copyOfRange(apiKey, 1, apiKey.length);
                } else {
                    apiKey = new String[apiKeyBackup.length];
                    System.arraycopy(apiKeyBackup, 0, apiKey, 0, apiKeyBackup.length);
                    try {
                        Thread.sleep(3200000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                return getRowData(commitUrl);
            }

        }

        return null;
    }

    private RequestEntity<Void> setUpCallMeta(String commitUrl) {
        HttpHeaders headers = new HttpHeaders();
        if (this.apiKey.length == 0) {
            apiKey = new String[apiKeyBackup.length];
            System.arraycopy(apiKeyBackup, 0, apiKey, 0, apiKeyBackup.length);
        }
        currentKey = apiKey[0];
        headers.set("Authorization", currentKey);
        RequestEntity<Void> requestEntity;
        try {
            requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(commitUrl));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }
        return requestEntity;
    }


    public String getHtmlData(String commitUrl) {
        RequestEntity<Void> requestEntity = setUpCallMeta(commitUrl);
        if (requestEntity == null) return null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(requestEntity, String.class);
            return response.getBody();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }
}
