package com.research.qmodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.AGraph;
import com.research.qmodel.model.Commit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
public class BasicQueryService extends GitHubIssuesFetcher {
  final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final Logger LOGGER = LoggerFactory.getLogger(BasicQueryService.class);

  @Value("${app.mode}")
  private String mode;

  @Value("${qmodel.api.key}")
  private String[] apiKey;

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

  // TODO figure out how to handle actions
  /*    public <T> T retrievemetrics(String url, TypeReference<T> targetType) {
      List<T> allEntities = new ArrayList<>();
      int pageNumber = 1;
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
  }*/
  public <T> T retrievemetrics(
      String url,
      TypeReference<T> targetType,
      String urlCondition,
      String pageCondition,
      boolean withCoursor) {
    List<JsonNode> allEntities = new ArrayList<>();
    int pageNumber = 1;
    if (withCoursor) {
      try {
        JsonNode body = getRowDataWithCursor(url);
        List<JsonNode> rowBody = objectMapper.convertValue(body, new TypeReference<>() {});
        allEntities.addAll(rowBody);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    while (isRun(pageNumber) && !withCoursor) {
      JsonNode body =
          getRowData(url, BASE_URL, pageNumber, PAGE_SIZE, urlCondition, pageCondition, false);
      if (body != null && !body.isEmpty()) {
        List<JsonNode> rowBody = objectMapper.convertValue(body, new TypeReference<>() {});
        allEntities.addAll(rowBody);
        LOGGER.info("getting page number {}", pageNumber);
        pageNumber++;
      } else {
        break;
      }
    }
    return objectMapper.convertValue(allEntities, targetType);
  }

  public <T> T retrievemetricsObject(
      String url, TypeReference<T> targetType, String urlCondition, String pageCondition) {
    List<JsonNode> allEntities = new ArrayList<>();
    int pageNumber = 1;

    JsonNode body =
        getRowData(url, BASE_URL, pageNumber, PAGE_SIZE, urlCondition, pageCondition, true);
    if (body != null && !body.isEmpty()) {
      JsonNode rowBody = objectMapper.convertValue(body, new TypeReference<>() {});
      allEntities.add(rowBody);
    }

    return objectMapper.convertValue(allEntities, targetType);
  }

  private <T> List<List<T>> partitionList(List<T> list, int size) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      partitions.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return partitions;
  }

  private boolean isRun(int pageNumber) {
    return !mode.equals("demo") || pageNumber < 4000;
  }

  private JsonNode getRowData(
      String url,
      String baseUrl,
      int pageNumber,
      int pageSize,
      String urlCondition,
      String pageCondition,
      boolean skipPages) {
    String apiUrl =
        skipPages
            ? String.format(url, baseUrl, "")
            : String.format(
                url,
                baseUrl,
                urlCondition + "page=" + pageNumber + pageCondition + "per_page=" + pageSize);
    HttpHeaders headers = new HttpHeaders();
    String currentKey = getNextKey(false);
    headers.set("Authorization", currentKey);
    RequestEntity<Void> requestEntity;
    try {
      requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(apiUrl));
      ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
      JsonNode body = response.getBody();
      return body;

    } catch (Exception e) {
      currentKey = getNextKey(true);
      headers.set("Authorization", currentKey);
      try {
        requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(apiUrl));
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
        return response.getBody();
      } catch (Exception ex) {
        LOGGER.error(ex.getMessage(), ex);
      }
    }
    return null;
  }

  public JsonNode getRowDataWithCursor(String commitUrl) {
    RequestEntity<Void> requestEntity = setUpCallMeta(commitUrl, true);
    if (requestEntity == null) return null;
    try {
      return getRowData(requestEntity.getUrl().toString(), commitUrl.split("/")[1], commitUrl.split("/")[2]);

    } catch (Exception e) {
      getNextKey(true);
      LOGGER.error(e.getMessage());
    }

    return null;
  }

  public JsonNode getRowData(String commitUrl) {
    RequestEntity<Void> requestEntity = setUpCallMeta(commitUrl);
    if (requestEntity == null) return null;
    try {
      ResponseEntity<JsonNode> response = restTemplate.exchange(requestEntity, JsonNode.class);
      return response.getBody();
    } catch (Exception e) {
      getNextKey(true);
      LOGGER.error(e.getMessage());
    }

    return null;
  }

  private RequestEntity<Void> setUpCallMeta(String commitUrl) {
    HttpHeaders headers = new HttpHeaders();
    String currentKey = getNextKey(false);
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

  String parseToCommitUrl(String urlFormat) {
    // Format the base URL
    String url = String.format(urlFormat, "https://api.github.com/", "");

    // String commitUrl = String.format(urlFormat, "", projOwner, projName);

    return url;
  }

  private RequestEntity<Void> setUpCallMeta(String commitUrl, boolean withCursor) {
    HttpHeaders headers = new HttpHeaders();
    String currentKey = getNextKey(false);
    headers.set("Authorization", currentKey);
    RequestEntity<Void> requestEntity;
    try {
      String url = parseToCommitUrl(commitUrl);
      requestEntity = new RequestEntity<>(headers, HttpMethod.GET, new URI(url));
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
      getNextKey(true);
    }
    return null;
  }

  public AGraph retrieveForks(String owner, String repo, Set<String> forks) {
    AGraph latestAgraphSnapshot = null;
    for (String fork : forks) {
      String[] forkedName = fork.split("/");
      JsonNode branches =
          retrievemetrics(
              "%s" + String.format("repos/%s/%s/branches", forkedName[0], forkedName[1]) + "%s",
              new TypeReference<>() {},
              "?",
              "&",
              false);
      for (JsonNode branch : branches) {
        String commitId =
            StringUtils.substringAfterLast(branch.path("commit").path("url").asText(), "/");
        AGraph agraph =
            retrievemetricsObject(
                "%s" + String.format("repos/%s/%s/commits/%s", owner, repo, commitId) + "%s",
                new TypeReference<>() {},
                "?",
                "?");
        if (latestAgraphSnapshot == null) {
          latestAgraphSnapshot = agraph;
        } else {
          for (Commit commit : agraph.getCommits()) {
            latestAgraphSnapshot.addCommit(commit);
          }
        }
      }
    }
    return latestAgraphSnapshot;
  }

  public AGraph retrieveCommitBySha(String owner, String repo, String sha) {

    return retrievemetricsObject(
        "%s" + String.format("repos/%s/%s/commits/%s", owner, repo, sha) + "%s",
        new TypeReference<>() {},
        "?",
        "&");
  }
}
