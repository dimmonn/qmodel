package com.research.qmodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;


@Service
public class BasicQueryService {
    final RestTemplate restTemplate;

    public BasicQueryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> T retrievemetrics(String url, TypeReference<T> targetType) throws URISyntaxException {
        List<JsonNode> allEntities = new ArrayList<>();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer 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");
        int pageNumber = 1;
        int pageSize = 100;
        String baseUrl = "https://api.github.com/";
        while (true) {
            String apiUrl = String.format(url, baseUrl, "?page=" + pageNumber + "&per_page=" + pageSize);
            RequestEntity<Void> requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(apiUrl));
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
            JsonNode body = response.getBody();
            if (body != null && body.size() > 0 && pageNumber < 101) {
                List<JsonNode> rowBody = new ObjectMapper().convertValue(body, ArrayList.class);
                allEntities.addAll(rowBody);
                System.out.println(pageNumber);
                pageNumber++;
            } else {
                break;
            }
        }
        return new ObjectMapper().convertValue(allEntities, targetType);
    }
}
