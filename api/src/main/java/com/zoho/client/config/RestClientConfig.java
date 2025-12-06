package com.zoho.client.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.io.IOException;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder() {

        // Enable buffering so we can read response body in logs
        var factory = new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory());

        // Custom interceptor
        ClientHttpRequestInterceptor loggingInterceptor = new ClientHttpRequestInterceptor() {
            @Override
            public org.springframework.http.client.ClientHttpResponse intercept(
                    org.springframework.http.HttpRequest request,
                    byte[] body,
                    ClientHttpRequestExecution execution) throws IOException {

                // Log request
                System.out.println("➡ REQUEST: " + request.getMethod() + " " + request.getURI());
                System.out.println("Headers: " + request.getHeaders());

                var response = execution.execute(request, body);

                // Log response
                System.out.println("⬅ RESPONSE: " + response.getStatusCode());

                return response;
            }
        };

        return RestClient.builder()
                .requestFactory(factory)
                .requestInterceptor(loggingInterceptor);
    }

    @Bean
    public CorsFilter corsFilter() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
