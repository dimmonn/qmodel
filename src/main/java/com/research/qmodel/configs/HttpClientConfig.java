package com.research.qmodel.configs;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

@Configuration
public class HttpClientConfig {

    @Value("${http.client.request.timeout:3000}")
    private int REQUEST_TIMEOUT;
    @Value("${http.client.connect.timeout:3000}")
    private int CONNECT_TIMEOUT;
    @Value("${http.client.socket.timeout:3000}")
    private int SOCKET_TIMEOUT;
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfig.class);

    public CloseableHttpClient httpClient() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(REQUEST_TIMEOUT))
                .setResponseTimeout(Timeout.ofSeconds(CONNECT_TIMEOUT))
                .build();
        return clientBuilder.setDefaultRequestConfig(requestConfig)
                .build();

    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient());
        return clientHttpRequestFactory;
    }

}
