package com.zoho.client.scheduler;

import com.zoho.client.model.ZohoTokenResponse;
import com.zoho.client.service.TokenStorage;
import com.zoho.client.service.TokenStore;
import com.zoho.client.service.ZohoAuthService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TokenRefreshScheduler {

    //private final TokenStore tokenStore;
    private final TokenStorage tokenStore;
    private final ZohoAuthService zohoAuthService;

    public TokenRefreshScheduler(TokenStorage tokenStore, ZohoAuthService authService) {
        this.tokenStore = tokenStore;
        this.zohoAuthService = authService;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // Every 5 minutes
    public void refreshBeforeExpiry() {
        ZohoTokenResponse token = tokenStore.getToken();

        if (token == null) return;

        long now = System.currentTimeMillis();
        long age = now - token.getFetchedAt();
        long lifetime = token.getExpires_in() * 1000L;
        long remaining = lifetime - age;

        // Refresh if less than 2 minutes remain
        if (remaining < 2 * 60 * 1000L) {
            System.out.println("🔄 Auto-refreshing Zoho token...");
            try {
                zohoAuthService.refreshToken();
                System.out.println("✅ Token refreshed successfully.");
            } catch (Exception ex) {
                System.err.println("❌ Auto-refresh failed: " + ex.getMessage());
            }
        }
    }
}
