// src/main/java/com/zoho/client/service/LineItemAiService.java
package com.zoho.client.service;


import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.ZohoItem;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LineItemAiService {

    private final RestClient restClient;
    private final ZohoProperties props;
    private final ZohoItemCacheService itemCache;
    private final ObjectMapper mapper = new ObjectMapper();

    public LineItemAiService(RestClient.Builder builder,
                             ZohoProperties props,
                             ZohoItemCacheService itemCache) {
        this.restClient = builder.build();
        this.props = props;
        this.itemCache = itemCache;
    }

    public JsonNode generateLineItems(double amount) throws Exception {
        List<ZohoItem> items = itemCache.getItems();

        Map<String, Object> req = new HashMap<>();
        req.put("model", "gpt-4.1");

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an assistant that generates Zoho Books invoice line_items JSON.\n");
        systemPrompt.append("Rules:\n");
        systemPrompt.append("1. Output ONLY a JSON object with a 'line_items' array.\n");
        systemPrompt.append("2. Use items from the provided list.\n");
        systemPrompt.append("3. Ensure the sum of (rate * quantity) equals exactly ")
                .append(amount).append(".\n");
        systemPrompt.append("4. If amount > 50, add a separate 'travel cost' line item.\n");
        systemPrompt.append("5. If data is missing, leave fields blank.\n");

        StringBuilder itemList = new StringBuilder("Available items:\n");
        for (ZohoItem it : items) {
            itemList.append("- item_id: ").append(it.getItem_id())
                    .append(", name: ").append(it.getName())
                    .append(", rate: ").append(it.getRate())
                    .append("\n");
        }

        Map<String, String> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt.toString());

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content",
                "Transaction amount is " + amount + ".\n" +
                        itemList + "\n" +
                        "Return JSON like: { \"line_items\": [ { \"item_id\": \"...\", \"name\": \"...\", \"rate\": 10, \"quantity\": 2, \"description\": \"...\", \"discount\": 0 } ] }"
        );

        req.put("messages", List.of(sysMsg, userMsg));

        String response = restClient.post()
                .uri(props.getOpenaiApiUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + props.getOpenaiApiKey())
                .body(req)
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(response);
        String content = root.path("choices").get(0)
                .path("message").path("content").asText();

        // content should be JSON string; parse it
        return mapper.readTree(content);
    }
}
