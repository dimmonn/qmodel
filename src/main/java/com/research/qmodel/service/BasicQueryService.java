package com.research.qmodel.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.research.qmodel.model.Commit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
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
public class BasicQueryService extends BasicKeyManager {
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
  public <T> T retrievemetrics(String url, TypeReference<T> targetType) {
    List<JsonNode> allEntities = new ArrayList<>();
    int pageNumber = 1;
    while (isRun(pageNumber)) {
      JsonNode body = getRowData(url, BASE_URL, pageNumber, PAGE_SIZE);
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

  private <T> List<List<T>> partitionList(List<T> list, int size) {
    List<List<T>> partitions = new ArrayList<>();
    for (int i = 0; i < list.size(); i += size) {
      partitions.add(list.subList(i, Math.min(i + size, list.size())));
    }
    return partitions;
  }

  private boolean isRun(int pageNumber) {
    return mode.equals("demo") ? pageNumber < 600000 : true;
  }

  private JsonNode getRowData(String url, String baseUrl, int pageNumber, int pageSize) {
    String apiUrl = String.format(url, baseUrl, "?page=" + pageNumber + "&per_page=" + pageSize);
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
        JsonNode body = response.getBody();
        return body;
      } catch (URISyntaxException ex) {
        LOGGER.error(ex.getMessage(), ex);
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

  public static int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> cache = new HashMap<>();

    for (int i = 0; i < nums.length; i++) {
      int delta = target - nums[i];
      if (cache.containsKey(target - delta)) {
        return new int[] {cache.get(target - delta), i};
      }
      cache.put(delta, i);
    }
    return null;
  }

  // Input: s = "a b c abc bb"
  // Output: 3
  // Explanation: The answer is "abc", with the length of 3.
  public static int lengthOfLongestSubstring(String s) {
    char[] letters = s.toCharArray();
    int[] trackingDublicates = new int[26];
    int left = 0;
    int result = 0;
    for (int right = 0; right < letters.length; right++) {
      // pw wke w
      // abcabcbb
      if (trackingDublicates[(int) letters[right] - 'a'] >= 1) {
        if (letters[right] == letters[right - 1]) {
          left = right;
          continue;
        }

        left++;
      }

      trackingDublicates[(int) letters[right] - 'a']++;
      result = Math.max(result, right - left + 1);
    }
    return result;
  }

  public int lengthOfLongestSubstring1(String s) {
    // Map to store the last index of each character.
    Map<Character, Integer> charIndexMap = new HashMap<>();
    int maxLength = 0;
    int left = 0; // Left pointer of the sliding window.

    // Iterate through the string with the right pointer.
    for (int right = 0; right < s.length(); right++) {
      char currentChar = s.charAt(right);
      //abcabcbb
      // If the character already exists in the window, move the left pointer.
      if (charIndexMap.containsKey(currentChar)) {
        // Move left pointer to the position after the last occurrence.
        left = Math.max(left, charIndexMap.get(currentChar) + 1);
      }

      // Update the last index of the current character.
      charIndexMap.put(currentChar, right);

      // Calculate the length of the current valid window and update maxLength.
      maxLength = Math.max(maxLength, right - left + 1);
    }

    return maxLength;
  }

  public static void main(String[] args) {
    int i = lengthOfLongestSubstring("abcabcbb");
    System.out.println(i);
  }
}
