package com.zoho.client.scheduler;

import com.zoho.client.model.ZohoTokenResponse;
import com.zoho.client.service.TokenStorage;
import com.zoho.client.service.ZohoAuthService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenRefreshScheduler {

    private final TokenStorage storage;
    private final ZohoAuthService authService;

    public TokenRefreshScheduler(TokenStorage storage, ZohoAuthService authService) {
        this.storage = storage;
        this.authService = authService;
    }

    // Run every 5 minutes
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void refreshIfCloseToExpiry() {
        ZohoTokenResponse token = storage.getToken();
        if (token == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long age = now - token.getFetchedAt();
        long lifetime = token.getExpires_in() * 1000L;

        // Refresh if less than 2 minutes remaining
        if (lifetime - age < 2 * 60 * 1000L) {
            System.out.println("🔄 Auto-refreshing Zoho token...");
            try {
                authService.refreshToken();
            } catch (Exception e) {
                System.err.println("❌ Auto-refresh error: " + e.getMessage());
            }
        }
    }
}
