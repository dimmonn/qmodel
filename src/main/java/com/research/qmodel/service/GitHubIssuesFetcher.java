package com.research.qmodel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.annotations.FileChangesDeserializer;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public abstract class GitHubIssuesFetcher extends BasicKeyManager {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    private final Logger LOGGER = LoggerFactory.getLogger(GitHubIssuesFetcher.class);
    @Value("${app.base_url:https://api.github.com/}")
    private String BASE_URL;

    private static String sendGetRequest(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

        int responseCode = connection.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("HTTP GET Request Failed with Error code : " + responseCode);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    private static String getNextUrl(HttpURLConnection connection) {
        String linkHeader = connection.getHeaderField("Link");
        if (linkHeader == null) {
            return null;
        }

        String[] parts = linkHeader.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.endsWith("rel=\"next\"") || trimmed.contains("rel=\"next\"")) {
                int start = trimmed.indexOf('<');
                int end = trimmed.indexOf('>');
                if (start >= 0 && end > start) {
                    return trimmed.substring(start + 1, end);
                }
            }
        }
        return null;
    }

    JsonNode getRowData(String urlFormat, String projOwner, String projName) throws Exception {
        String url = String.format(urlFormat, BASE_URL, projOwner, projName);

        List<JsonNode> allIssues = new ArrayList<>();
        String nextUrl = url;
        int processedIssue = 1;
        int skip = 1;
        while (nextUrl != null) {

            String currentKey = getNextKey(false);
            HttpURLConnection connection = (HttpURLConnection) new URL(nextUrl).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("Authorization", currentKey);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("HTTP GET Request Failed with Error code : " + responseCode);
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JsonNode issuesPage = objectMapper.readTree(response.toString());
            for (JsonNode issue : issuesPage) {
                if (!StringUtils.isBlank(issue.path("pull_request").toString())) {
                    LOGGER.warn("Skipping issue #{}: {}", skip, issue.path("url"));
                    skip++;
                    continue;
                }
                LOGGER.info("Pulling issue #{}: {}", processedIssue, issue.path("url"));
                allIssues.add(issue);
                processedIssue++;
            }
            nextUrl = getNextUrl(connection);
        }

        LOGGER.info("Issues are collected and are sent to a further processing.pi");
        return objectMapper.valueToTree(allIssues);
    }
}
