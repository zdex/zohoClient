package com.zoho.client.config;

import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, ZohoTokenResponse> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ZohoTokenResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use Java serialization (works because ZohoTokenResponse implements Serializable)
        template.afterPropertiesSet();
        return template;
    }
}
