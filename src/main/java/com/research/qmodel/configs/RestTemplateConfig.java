package com.research.qmodel.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.SpringHandlerInstantiator;
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
    public HandlerInstantiator handlerInstantiator(ApplicationContext context) {
        return new SpringHandlerInstantiator(context.getAutowireCapableBeanFactory());
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public ObjectMapper objectMapper(HandlerInstantiator handlerInstantiator) {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.handlerInstantiator(handlerInstantiator);
        return builder.build();
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public RestTemplate restTemplate() {
        return new RestTemplate(httpComponentsClientHttpRequestFactory);
    }
}
