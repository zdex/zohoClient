// src/main/java/com/zoho/client/service/ZohoContactService.java
package com.zoho.client.service;


import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ZohoContactsService {

    private final RestClient restClient;
    private final ZohoProperties props;
    private final ZohoAuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TokenStorage tokenStore;
    public ZohoContactsService(RestClient.Builder builder,
                              ZohoProperties props,
                              ZohoAuthService authService, TokenStorage store) {
        this.restClient = builder.build();
        this.props = props;
        this.authService = authService;
        this.tokenStore = store;
    }

    public String getOrCreateContactId(String customerName) throws Exception {
        ZohoTokenResponse token = authService.getValidToken();

        String searchUrl = props.getBooksBaseUrl()
                + "/contacts?organization_id=" + props.getOrganizationId()
                + "&contact_name_contains=" + customerName;

        String response = restClient.get()
                .uri(searchUrl)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(response);
        JsonNode contacts = root.get("contacts");
        if (contacts != null && contacts.isArray() && contacts.size() > 0) {
            return contacts.get(0).path("contact_id").asText();
        }

        // Create new contact
        String createUrl = props.getBooksBaseUrl()
                + "/contacts?organization_id=" + props.getOrganizationId();

        Map<String, Object> body = new HashMap<>();
        body.put("contact_name", customerName);
        body.put("contact_type", "customer");

        String createResponse = restClient.post()
                .uri(createUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode createdRoot = mapper.readTree(createResponse);
        JsonNode contactNode = createdRoot.path("contact");
        return contactNode.path("contact_id").asText();
    }

    public List<Object> fetchContacts() {
        ZohoTokenResponse token = tokenStore.getToken();

        String url = props.getBooksBaseUrl()
                + "/contacts?organization_id=" + props.getOrganizationId();

        String response = restClient.get()
                .uri(url)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(response);
        return mapper.convertValue(root.get("contacts"), List.class);
    }

    public void uploadContactsCsv(MultipartFile file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
            String line;

            ZohoTokenResponse token = tokenStore.getToken();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");

                Map<String, Object> body = new HashMap<>();
                body.put("contact_name", parts[0]);
                body.put("email", parts[1]);
                body.put("company_name", parts[2]);

                createContact(token.getAccess_token(), body);
            }

        } catch (Exception e) {
            throw new RuntimeException("CSV upload failed", e);
        }
    }

    private void createContact(String token, Map<String, Object> contact) {
        String url = props.getBooksBaseUrl()
                + "/contacts?organization_id=" + props.getOrganizationId();

        restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Zoho-oauthtoken " + token)
                .body(contact)
                .retrieve()
                .toBodilessEntity();
    }
}
