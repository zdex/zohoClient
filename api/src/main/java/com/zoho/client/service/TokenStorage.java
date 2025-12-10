package com.zoho.client.service;

import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.stereotype.Component;

@Component
public class TokenStorage {

    private ZohoTokenResponse token; // stored in memory
    private long expiryEpoch = 0;

    public ZohoTokenResponse getToken() {
        return token;
    }

    public void saveToken(ZohoTokenResponse token) {
        this.token = token;
        int time = (token.getExpires_in()-60) * 1000;
        this.expiryEpoch = System.currentTimeMillis() + time;
        //this.expiryEpoch = Long.MAX_VALUE;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiryEpoch;
        //return false;
    }

    public void clear() {
        token = null;
        expiryEpoch = 0;
    }
}
