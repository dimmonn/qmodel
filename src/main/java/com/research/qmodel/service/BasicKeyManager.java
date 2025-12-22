package com.research.qmodel.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public abstract class BasicKeyManager {
    @Value("${qmodel.api.key}")
    private String[] apiKey;

    private int currentIndex;
    private final Logger LOGGER = LoggerFactory.getLogger(BasicKeyManager.class);

    public String getNextKey(boolean isExpired) {
        if (apiKey.length <= currentIndex) {
            return null;
        }
        if (!isExpired) {
            return apiKey[currentIndex];
        }
        LOGGER.info("Switching over an expired key.");
        currentIndex =
                (currentIndex + 1)
                        % apiKey.length;
        String key = apiKey[currentIndex];
        return key;
    }
}
