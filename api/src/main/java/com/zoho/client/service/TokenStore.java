package com.zoho.client.service;

import com.zoho.client.model.ZohoTokenResponse;

public interface TokenStore {
    ZohoTokenResponse getToken();
    void saveToken(ZohoTokenResponse token);
    boolean hasValidToken();
}
