package com.nti.nti_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CorsConfig {
    // CORS налаштований в SecurityConfig
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}