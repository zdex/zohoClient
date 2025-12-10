package com.zoho.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zoho")
public class ZohoProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authUrl;
    private String tokenUrl;
    private String scope;

    private String booksBaseUrl = "https://www.zohoapis.com/books/v3";

    // ZohoProperties.java
    private String organizationId; // remove this OR leave null initially

    private String openaiApiKey;
    private String openaiApiUrl = "https://api.openai.com/v1/chat/completions";

    private String currencyId = "USD";
    private String paymentModeDefault = "Cash";

    public String getOrganizationId() { return organizationId; }
    public void setOrganizationId(String organizationId) { this.organizationId = organizationId; }


    public String getBooksBaseUrl() {
        return booksBaseUrl;
    }

    public void setBooksBaseUrl(String booksBaseUrl) {
        this.booksBaseUrl = booksBaseUrl;
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getAuthUrl() { return authUrl; }
    public void setAuthUrl(String authUrl) { this.authUrl = authUrl; }

    public String getTokenUrl() { return tokenUrl; }
    public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public void setOpenaiApiKey(String openaiApiKey) {
        this.openaiApiKey = openaiApiKey;
    }

    public String getOpenaiApiUrl() {
        return openaiApiUrl;
    }

    public void setOpenaiApiUrl(String openaiApiUrl) {
        this.openaiApiUrl = openaiApiUrl;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public String getPaymentModeDefault() {
        return paymentModeDefault;
    }

    public void setPaymentModeDefault(String paymentModeDefault) {
        this.paymentModeDefault = paymentModeDefault;
    }
}
