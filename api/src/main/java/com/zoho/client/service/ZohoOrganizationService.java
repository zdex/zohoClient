package com.zoho.client.service;


import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ZohoOrganizationService {

    private final RestClient restClient;
    private final ZohoProperties props;
    private final TokenStorage tokenStore;
    private String cachedOrgId = null;

    public ZohoOrganizationService(RestClient.Builder builder,
                                   ZohoProperties props, TokenStorage store) {
        this.restClient = builder.build();

        this.props = props;
        this.tokenStore = store;
    }

    public String getOrganizationId() throws Exception {
        if (cachedOrgId != null) return cachedOrgId;

        ZohoTokenResponse token = tokenStore.getToken();

        String url = props.getBooksBaseUrl() + "/organizations";

        String resp = restClient.get()
                .uri(url)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .body(String.class);

        JsonNode root = new ObjectMapper().readTree(resp);
        cachedOrgId = root.path("organizations").get(0).path("organization_id").asText();

        props.setOrganizationId(cachedOrgId);
        return cachedOrgId;
    }
}
