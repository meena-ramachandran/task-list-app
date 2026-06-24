package com.ortecfinance.tasklist.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskListOpenAPI() {
        return new OpenAPI().info(new Info().title("TaskList API").description("REST API for managing projects and tasks").version("1.0.0"));
    }
}

