package com.research.qmodel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public abstract class BasicKeyManager {
  @Value("${qmodel.api.key}")
  private String[] apiKey;

  private int currentIndex;

  public String getNextKey(boolean isExpired) {
    if (!isExpired) {
      return apiKey[currentIndex];
    }
    String key = apiKey[currentIndex];
    currentIndex =
        (currentIndex + 1)
            % apiKey.length;
    return key;
  }
}
