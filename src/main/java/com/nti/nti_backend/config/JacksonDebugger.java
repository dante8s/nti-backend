package com.nti.nti_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class JacksonDebugger implements CommandLineRunner {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("DEBUG: The ObjectMapper being used is: " + objectMapper.getClass().getName());
        System.out.println("DEBUG: Default typing enabled? " + objectMapper.getPolymorphicTypeValidator());
    }
}