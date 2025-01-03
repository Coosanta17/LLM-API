package net.coosanta.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;


@Configuration
public class CorsConfig {
    @Value("${cors.allowed-origins:}") // Default to allow none
    private String[] allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String[] allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String[] allowedHeaders;

    @Value("${cors.allow-credentials:false}")
    private boolean allowCredentials;

    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }

    public String[] getAllowedMethods() {
        return allowedMethods;
    }

    public String[] getAllowedHeaders() {
        return allowedHeaders;
    }

    public boolean getAllowCredentials() {
        return allowCredentials;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(getAllowedOrigins()));
        config.setAllowedMethods(Arrays.asList(getAllowedMethods()));
        config.setAllowedHeaders(Arrays.asList(getAllowedHeaders()));
        config.setAllowCredentials(getAllowCredentials());
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

