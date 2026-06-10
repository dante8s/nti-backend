package com.nti.nti_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CorsConfig {
    // CORS is configured in SecurityConfig
    @Bean
    @Primary // This ensures this mapper is used for all REST controllers
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Essential configuration for clean JSON
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Explicitly DISABLE default typing to override any hidden property
        mapper.deactivateDefaultTyping();

        return mapper;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}