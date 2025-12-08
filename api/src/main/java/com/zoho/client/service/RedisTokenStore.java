package com.zoho.client.service;

import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RedisTokenStore implements TokenStore {

    private static final String KEY = "zoho:oauth:token";
    private final RedisTemplate<String, ZohoTokenResponse> redis;

    public RedisTokenStore(RedisTemplate<String, ZohoTokenResponse> redis) {
        this.redis = redis;
    }

    @Override
    public ZohoTokenResponse getToken() {
        return redis.opsForValue().get(KEY);
    }

    @Override
    public void saveToken(ZohoTokenResponse token) {
        if (token != null) {
            token.setFetchedAt(System.currentTimeMillis());
        }
        redis.opsForValue().set(KEY, token);
    }

    @Override
    public boolean hasValidToken() {
        ZohoTokenResponse token = getToken();
        return token != null && !token.isExpired();
    }
}
