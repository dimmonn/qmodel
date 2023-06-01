package com.research.qmodel.configs;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
@Scope(SCOPE_PROTOTYPE)
public class RestTemplateConfig {
    private final HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory;

    public RestTemplateConfig(HttpComponentsClientHttpRequestFactory httpComponentsClientHttpRequestFactory) {
        this.httpComponentsClientHttpRequestFactory = httpComponentsClientHttpRequestFactory;
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public RestTemplate restTemplate() {
        return new RestTemplate(httpComponentsClientHttpRequestFactory);
    }
}
