package com.zoho.client.service;

import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ZohoAuthService {

    private final RestClient restClient;
    private final ZohoProperties properties;
   // private final TokenStore tokenStore;
   private final TokenStorage tokenStore;
    private final ZohoOrganizationService orgService;
    private final ZohoItemCacheService itemCache;

    public ZohoAuthService(RestClient.Builder builder,
                           ZohoProperties properties,
                           TokenStorage store, ZohoOrganizationService orgService,
                           ZohoItemCacheService itemCache) {
        this.restClient = builder.build();
        this.properties = properties;
        this.tokenStore = store;
        this.orgService = orgService;
        this.itemCache = itemCache;
    }

    // 1) Build authorization URL for Angular to redirect browser
    public String buildAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString(properties.getAuthUrl())
                .queryParam("scope", properties.getScope())
                .queryParam("client_id", properties.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .build()
                .toString();
    }

    // 2) Exchange auth code for access + refresh tokens
    public ZohoTokenResponse exchangeCodeForToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("code", code);

        ZohoTokenResponse token = restClient.post()
                .uri(properties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(ZohoTokenResponse.class);

        tokenStore.saveToken(token);

        try {
            orgService.getOrganizationId();   // loads & caches orgId
            itemCache.loadItems();           // loads items AFTER orgId is known
            System.out.println("🚀 Initialization complete");
        }
        catch (Exception e) {
            System.err.println("Error initializing Zoho: " + e.getMessage());
        }
        return token;
    }

    // 3) Refresh using refresh_token
    public ZohoTokenResponse refreshToken() {
        ZohoTokenResponse current = tokenStore.getToken();
        if (current == null || current.getRefresh_token() == null) {
            throw new IllegalStateException("No refresh token available");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", current.getRefresh_token());
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        try {
            ZohoTokenResponse token = restClient.post()
                    .uri(properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(ZohoTokenResponse.class);

            tokenStore.saveToken(token);
            orgService.getOrganizationId();   // loads & caches orgId
            itemCache.loadItems();           // loads items AFTER orgId is known
            System.out.println("🚀 Initialization complete");
            return token;
        } catch (HttpClientErrorException e) {
            System.err.println("❌ Refresh failed: " + e.getResponseBodyAsString());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 4) Get valid token (refresh if needed)
    public ZohoTokenResponse getValidToken() {
        if (!tokenStore.isExpired()) {
            return tokenStore.getToken();
        }
        return refreshToken();
    }

    // For Angular to display token info safely
    public ZohoTokenResponse getCurrentTokenSnapshot() {
        return tokenStore.getToken();
    }
}
