package com.zoho.client.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ZohoTokenResponse {

    private String access_token;
    private String refresh_token;
    private String scope;
    private String api_domain;
    private String token_type;
    private int expires_in;

    @JsonIgnore
    private long fetchedAt = System.currentTimeMillis();

    public String getAccess_token() { return access_token; }
    public void setAccess_token(String access_token) { this.access_token = access_token; }

    public String getRefresh_token() { return refresh_token; }
    public void setRefresh_token(String refresh_token) { this.refresh_token = refresh_token; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public String getApi_domain() { return api_domain; }
    public void setApi_domain(String api_domain) { this.api_domain = api_domain; }

    public String getToken_type() { return token_type; }
    public void setToken_type(String token_type) { this.token_type = token_type; }

    public int getExpires_in() { return expires_in; }
    public void setExpires_in(int expires_in) { this.expires_in = expires_in; }

    public long getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }

    @JsonIgnore
    public boolean isExpired() {
        long now = System.currentTimeMillis();
        long lifetimeMs = expires_in * 1000L;
        // small safety buffer of 10 seconds
        return now >= fetchedAt + lifetimeMs - 10_000L;
    }

    @Override
    public String toString() {
        return "ZohoTokenResponse{" +
                "access_token='" + access_token + '\'' +
                ", refresh_token='" + refresh_token + '\'' +
                ", scope='" + scope + '\'' +
                ", api_domain='" + api_domain + '\'' +
                ", token_type='" + token_type + '\'' +
                ", expires_in=" + expires_in +
                '}';
    }
}
