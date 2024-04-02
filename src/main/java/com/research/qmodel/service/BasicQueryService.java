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
import java.util.ArrayList;
import java.util.List;


@Service
public class BasicQueryService {
    final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Logger LOGGER = LoggerFactory.getLogger(BasicQueryService.class);
    @Value("${app.mode}")
    private String mode;
    @Value("${qmodel.api.key}")
    private String apiKey;
    @Value("${app.batch_limit}")
    private int BATCH_LIMIT;
    @Value("${app.page_size}")
    private int PAGE_SIZE;
    @Value("${app.base_url}")
    private String BASE_URL;

    public BasicQueryService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T retrievemetrics(String url, TypeReference<T> targetType) {
        List<JsonNode> allEntities = new ArrayList<>();
        int pageNumber = 1;
        while (isRun(pageNumber)) {
            JsonNode body = getRowData(url, BASE_URL, pageNumber, PAGE_SIZE);
            if (body != null && body.size() > 0) {
                List<JsonNode> rowBody = new ObjectMapper().convertValue(body, ArrayList.class);
                allEntities.addAll(rowBody);
                LOGGER.info("getting page number {}", pageNumber);
                pageNumber++;
            } else {
                break;
            }
        }
        return objectMapper.convertValue(allEntities, targetType);
    }

    private boolean isRun(int pageNumber) {
        return mode.equals("demo") ? pageNumber < BATCH_LIMIT : true;
    }

    private JsonNode getRowData(String url, String baseUrl, int pageNumber, int pageSize) {
        String apiUrl = String.format(url, baseUrl, "?page=" + pageNumber + "&per_page=" + pageSize);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        RequestEntity<Void> requestEntity;
        try {
            requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(apiUrl));
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
            JsonNode body = response.getBody();
            return body;
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return null;
        }

    }

    public JsonNode getRowData(String commitUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", apiKey);
        RequestEntity<Void> requestEntity;
        try {
            requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(commitUrl));
        } catch (Exception e) {
            return null;
        }
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }

    }
}
