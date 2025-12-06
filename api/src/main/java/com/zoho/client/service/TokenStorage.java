package com.zoho.client.service;

import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantLock;

@Component
public class TokenStorage {

    private ZohoTokenResponse token;
    private final ReentrantLock lock = new ReentrantLock();

    public ZohoTokenResponse getToken() {
        lock.lock();
        try {
            return token;
        } finally {
            lock.unlock();
        }
    }

    public void saveToken(ZohoTokenResponse token) {
        lock.lock();
        try {
            if (token != null) {
                token.setFetchedAt(System.currentTimeMillis());
            }
            this.token = token;
        } finally {
            lock.unlock();
        }
    }

    public boolean hasValidToken() {
        lock.lock();
        try {
            return token != null && !token.isExpired();
        } finally {
            lock.unlock();
        }
    }
}
