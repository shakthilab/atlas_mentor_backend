package com.lab.atlasmentor.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // CORS is already configured in SecurityConfig.java
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
