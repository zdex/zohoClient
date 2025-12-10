package com.zoho.client.service;

import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.ZohoItem;
import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class ZohoItemCacheService {

    private final RestClient restClient;

    private final ZohoOrganizationService orgService;
    private final ZohoProperties props;
    private final TokenStorage tokenStore;
    private final ObjectMapper mapper = new ObjectMapper();

    // IN-MEMORY cache
    private final List<ZohoItem> cachedItems = new ArrayList<>();

    public ZohoItemCacheService(RestClient.Builder builder,
                                TokenStorage store,
                                ZohoOrganizationService orgService,
                                ZohoProperties props) {
        this.restClient = builder.build();
        this.tokenStore = store;
        this.orgService = orgService;
        this.props = props;
    }

    /**
     * Loads items from Zoho Books only once (called at startup).
     */
    public void loadItems() throws Exception {

        ZohoTokenResponse token = tokenStore.getToken();
        String orgId = orgService.getOrganizationId();

        String url = props.getBooksBaseUrl() + "/items?organization_id=" + orgId;

        String response = restClient.get()
                .uri(url)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(response);
        JsonNode itemsNode = root.path("items");

        cachedItems.clear();

        if (itemsNode.isArray()) {
            for (JsonNode node : itemsNode) {
                ZohoItem item = new ZohoItem();
                item.setItem_id(node.path("item_id").asText());
                item.setName(node.path("name").asText());
                item.setDescription(node.path("description").asText(""));
                item.setRate(node.path("rate").asDouble(0));
                cachedItems.add(item);
            }
        }

        System.out.println("✅ Loaded " + cachedItems.size() + " items into in-memory cache.");
    }

    /**
     * Returns cached items.
     */
    public List<ZohoItem> getItems() {
        return cachedItems;
    }
}
