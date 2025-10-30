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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public abstract class GitHubIssuesFetcher extends BasicKeyManager {
  @Autowired private RestTemplate restTemplate;
  private final Logger LOGGER = LoggerFactory.getLogger(GitHubIssuesFetcher.class);

  // Helper method to send a GET request and return the response as a string
  private static String sendGetRequest(String urlString) throws Exception {
    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

    // Handle authentication if needed (uncomment the line below and replace YOUR_GITHUB_TOKEN)
    // connection.setRequestProperty("Authorization", "Bearer YOUR_GITHUB_TOKEN");

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

  // Helper method to extract the 'next' URL from the Link header for pagination
  private static String getNextUrl(HttpURLConnection connection) {
    // Look for the "Link" header in the response, which contains pagination information
    String linkHeader = connection.getHeaderField("Link");
    if (linkHeader == null) {
      return null;
    }

    // The Link header can contain multiple comma-separated links, e.g.:
    // <url1>; rel="prev", <url2>; rel="next"
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
    return null; // No more pages
  }

  // Method to get all issues for a given project using pagination
  JsonNode getRowData(String urlFormat, String projOwner, String projName) throws Exception {
    // Format the URL by replacing the placeholders with actual values
    String url = String.format(urlFormat, "https://api.github.com/", projOwner, projName);

    List<JsonNode> allIssues = new ArrayList<>();
    String nextUrl = url;
    int processedIssue = 1;
    int skip = 1;
    while (nextUrl != null) {

      String currentKey = getNextKey(false);
      // Make the HTTP request
      HttpURLConnection connection = (HttpURLConnection) new URL(nextUrl).openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
      connection.setRequestProperty("Authorization", currentKey);

      // Handle authentication if needed
      // connection.setRequestProperty("Authorization", "Bearer YOUR_GITHUB_TOKEN");

      int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new RuntimeException("HTTP GET Request Failed with Error code : " + responseCode);
      }

      // Read the response body
      BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      StringBuilder response = new StringBuilder();
      String inputLine;
      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();

      // Parse the response body into a JsonNode
      ObjectMapper mapper = new ObjectMapper();
      JsonNode issuesPage = mapper.readTree(response.toString());
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

      // Check if there is a 'next' URL for pagination
      nextUrl = getNextUrl(connection);
    }

    LOGGER.info("Issues are collected and are sent to a further processing.pi");
    return new ObjectMapper().valueToTree(allIssues);
  }
}
