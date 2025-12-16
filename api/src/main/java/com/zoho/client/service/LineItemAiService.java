package com.zoho.client.service;


import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.ZohoItem;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        // REQUIRED active catalog items
        ZohoItem travelItem = findItemByName(items, props.getTravelItemName()); // "Travel Cost"
        ZohoItem tipItem = findItemByName(items, props.getTipItemName());       // "Tip"

        BigDecimal target = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);

        // ---------- Build OpenAI request ----------
        Map<String, Object> req = new HashMap<>();
        req.put("model", "gpt-4.1");

        StringBuilder itemList = new StringBuilder();
        for (ZohoItem it : items) {
            itemList.append("- item_id: ").append(it.getItem_id())
                    .append(", name: ").append(it.getName())
                    .append(", rate: ").append(it.getRate())
                    .append("\n");
        }

        String systemPrompt = """
You generate Zoho Books invoice line_items JSON.

RULES:
1. Output ONLY valid JSON.
2. JSON must contain exactly one key: "line_items".
3. Every line item MUST include a valid item_id from the provided list.
4. Use ONLY the provided items.
5. Do NOT include Travel or Tip items.
6. The sum of (rate × quantity) for all line_items excluding Travel or Tip items MUST be STRICTLY LESS THAN the transaction """ +  amount + """
7.  Do NOT try to balance totals.
""";

        Map<String, String> sysMsg = Map.of("role", "system", "content", systemPrompt);

        Map<String, String> userMsg = Map.of(
                "role", "user",
                "content",
                """
    Transaction amount: %s
    
    Generate base invoice items only.
    Leave some amount unallocated.
    
    Available active items:
    %s
    
    Return JSON strictly as:
    { "line_items": [ { "item_id":"", "name":"", "rate":0, "quantity":0, "description":"", "discount":0 } ] }
    """
                        .formatted(target, itemList)
        );

        req.put("messages", List.of(sysMsg, userMsg));

        // ---------- Call OpenAI ----------
        String response = restClient.post()
                .uri(props.getOpenaiApiUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + props.getOpenaiApiKey())
                .body(req)
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(response);
        String content = root.path("choices").get(0)
                .path("message")
                .path("content")
                .asText();

        JsonNode aiJson = mapper.readTree(content);
        ArrayNode lineItems = (ArrayNode) aiJson.path("line_items");

        if (lineItems == null || lineItems.isEmpty()) {
            throw new IllegalStateException("AI returned empty line_items");
        }

        // ---------- Validate AI base total ----------
        BigDecimal baseTotal = calculateTotal(lineItems);
        if (baseTotal.compareTo(target) >= 0) {
            throw new IllegalStateException("AI base total must be LESS than target");
        }

        // ---------- Add Travel Cost FIRST (rate = $1) ----------
        BigDecimal remaining = target.subtract(baseTotal);

        BigDecimal travelRate = BigDecimal.ONE; // ✅ $1 per unit
        BigDecimal travelQty = remaining.setScale(0, RoundingMode.DOWN);

        if (travelQty.compareTo(BigDecimal.ZERO) > 0) {
            addOrUpdateTravelLine(lineItems, travelItem, travelQty);
        }

        // ---------- Add Tip for decimal remainder ----------
        BigDecimal afterTravel = calculateTotal(lineItems);
        BigDecimal tipAmount = target.subtract(afterTravel).setScale(2, RoundingMode.HALF_UP);

        if (tipAmount.compareTo(BigDecimal.ZERO) > 0) {
            addTipLine(lineItems, tipItem, tipAmount);
        }
        if (!hasBaseItem(lineItems, travelItem, tipItem)) {
            throw new IllegalStateException(
                    "Invoice must contain at least one base service item. AI returned only adjustments."
            );
        }

        // ---------- Final validation ----------
        BigDecimal finalTotal = calculateTotal(lineItems);
        if (finalTotal.compareTo(target) != 0) {
            throw new IllegalStateException(
                    "Final total mismatch. Expected=" + target + ", actual=" + finalTotal
            );
        }

        return aiJson;
    }

    private BigDecimal calculateTotal(ArrayNode items) {
        BigDecimal sum = BigDecimal.ZERO;

        for (JsonNode n : items) {
            BigDecimal rate = BigDecimal.valueOf(n.path("rate").asDouble());
            BigDecimal qty  = BigDecimal.valueOf(n.path("quantity").asDouble());
            sum = sum.add(rate.multiply(qty));
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private void addOrUpdateTravelLine(
            ArrayNode items,
            ZohoItem travelItem,
            BigDecimal quantity
    ) {
        for (JsonNode n : items) {
            if (travelItem.getItem_id().equals(n.path("item_id").asText())) {
                ((ObjectNode) n).put("quantity", quantity.doubleValue());
                ((ObjectNode) n).put("rate", 1.0);
                return;
            }
        }

        ObjectNode travel = items.addObject();
        travel.put("item_id", travelItem.getItem_id());
        travel.put("name", travelItem.getName());
        travel.put("rate", 1.0); // ✅ $1 per unit
        travel.put("quantity", quantity.doubleValue());
        travel.put("description", "Travel cost adjustment");
        travel.put("discount", 0);
    }

    private void addTipLine(
            ArrayNode items,
            ZohoItem tipItem,
            BigDecimal tipAmount
    ) {
        ObjectNode tip = items.addObject();
        tip.put("item_id", tipItem.getItem_id());
        tip.put("name", tipItem.getName());
        tip.put("rate", tipAmount.doubleValue());
        tip.put("quantity", 1);
        tip.put("description", "Tip");
        tip.put("discount", 0);
    }


    private void validateLineItemTotalBigDecimal(ArrayNode lineItems, BigDecimal expected) {
        BigDecimal sum = BigDecimal.ZERO;

        for (JsonNode item : lineItems) {
            BigDecimal rate = BigDecimal.valueOf(item.path("rate").asDouble(0)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal qty  = BigDecimal.valueOf(item.path("quantity").asDouble(0)).setScale(3, RoundingMode.HALF_UP);
            sum = sum.add(rate.multiply(qty));
        }

        // allow tiny epsilon due to qty scale
        if (sum.subtract(expected).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalStateException("Generated line_items total mismatch. Expected=" + expected + ", actual=" + sum);
        }
    }


    private ZohoItem findItemByName(List<ZohoItem> items, String name) {
        return items.stream()
                .filter(i -> i.getName() != null && i.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("Active Zoho item not found: " + name)
                );
    }

    private boolean hasBaseItem(ArrayNode items, ZohoItem travel, ZohoItem tip) {
        for (JsonNode n : items) {
            String itemId = n.path("item_id").asText("");
            if (!itemId.equals(travel.getItem_id()) &&
                    !itemId.equals(tip.getItem_id())) {
                return true;
            }
        }
        return false;
    }


    private void enforceTotalUsingTravel(ArrayNode lineItems,
                                         BigDecimal targetAmount,
                                         String travelItemId,
                                         BigDecimal travelRate) {

        ObjectNode travelNode = null;
        BigDecimal current = BigDecimal.ZERO;

        for (JsonNode n : lineItems) {
            ObjectNode li = (ObjectNode) n;

            // enforce item_id always present
            if (!li.hasNonNull("item_id") || li.path("item_id").asText().isBlank()) {
                throw new IllegalStateException("Line item missing item_id: " + li);
            }

            BigDecimal rate = BigDecimal.valueOf(li.path("rate").asDouble(0));
            BigDecimal qty  = BigDecimal.valueOf(li.path("quantity").asDouble(0));
            current = current.add(rate.multiply(qty));

            if (travelItemId.equals(li.path("item_id").asText())) {
                travelNode = li;
            }
        }

        if (travelNode == null) {
            throw new IllegalStateException("Travel line item missing. Travel item_id=" + travelItemId);
        }

        BigDecimal diff = targetAmount.subtract(current);

        // already matches
        if (diff.abs().compareTo(new BigDecimal("0.001")) <= 0) {
            return;
        }

        // Adjust travel quantity: deltaQty = diff / travelRate
        BigDecimal deltaQty = diff.divide(travelRate, 3, RoundingMode.HALF_UP);

        BigDecimal existingQty = BigDecimal.valueOf(travelNode.path("quantity").asDouble(0));
        BigDecimal newQty = existingQty.add(deltaQty);

        // do not allow negative miles
        if (newQty.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Cannot adjust travel quantity negative. Needed=" + newQty);
        }

        travelNode.put("quantity", newQty.doubleValue());
    }

/*
    private void sanitizeLineItems(ArrayNode lineItems) {

        for (JsonNode item : lineItems) {
            String name = item.path("name").asText("").toLowerCase();

            boolean shouldNotHaveItemId =
                    name.contains("tip")
                            || name.contains("travel")
                            || name.contains("adjustment")
                            || name.contains("service")
                            || name.contains("charge")
                            || name.contains("round");

            if (shouldNotHaveItemId) {
                ((ObjectNode) item).remove("item_id");
            }
        }
    }

    private void normalizeLineItemTotal(ArrayNode lineItems, double targetAmount) {

        BigDecimal target = BigDecimal.valueOf(targetAmount);
        BigDecimal current = BigDecimal.ZERO;

        ObjectNode travelItem = null;

        for (JsonNode item : lineItems) {
            BigDecimal rate = BigDecimal.valueOf(item.path("rate").asDouble(0));
            BigDecimal qty  = BigDecimal.valueOf(item.path("quantity").asDouble(0));
            current = current.add(rate.multiply(qty));

            if (item.path("name").asText("").equalsIgnoreCase("Travel Cost")) {
                travelItem = (ObjectNode) item;
            }
        }

        BigDecimal diff = target.subtract(current);

        // 🎯 Case 1: Exact match
        if (diff.abs().compareTo(new BigDecimal("0.001")) <= 0) {
            return;
        }

        // 🎯 Case 2: Adjust Travel Cost quantity
        if (travelItem != null) {
            BigDecimal rate = BigDecimal.valueOf(travelItem.path("rate").asDouble(1));
            BigDecimal deltaQty = diff.divide(rate, 3, RoundingMode.HALF_UP);

            BigDecimal newQty = BigDecimal.valueOf(travelItem.path("quantity").asDouble(0))
                    .add(deltaQty);

            travelItem.put("quantity", newQty.doubleValue());
            return;
        }

        // 🎯 Case 3: No travel → create adjustment item
       *//* ObjectNode adj = lineItems.addObject();
        adj.put("name", "Adjustment");
        adj.put("quantity", 1);
        adj.put("rate", diff.doubleValue());
        adj.put("description", "Auto-adjusted to match transaction total");*//*
    }

    private void validateLineItemTotal(JsonNode lineItems, double expectedAmount) {

        double sum = 0;

        for (JsonNode item : lineItems) {
            double rate = item.path("rate").asDouble(0);
            double qty = item.path("quantity").asDouble(0);
            sum += rate * qty;
        }

        if (Math.abs(sum - expectedAmount) > 0.001) {
            throw new IllegalStateException(
                    "Generated line_items total mismatch. Expected="
                            + expectedAmount + ", actual=" + sum
            );
        }
    }*/

    /*public JsonNode generateLineItems(double amount) throws Exception {
        List<ZohoItem> items = itemCache.getItems();

        Map<String, Object> req = new HashMap<>();
        req.put("model", "gpt-4.1");

        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an assistant that generates Zoho Books invoice line_items JSON.\n");
        systemPrompt.append("Rules:\n");
        systemPrompt.append("1. Output ONLY a JSON object with a 'line_items' array.\n");
        systemPrompt.append("2. Use items from the provided list.\n");
        systemPrompt.append("5. If ");
        systemPrompt.append(amount);
        systemPrompt.append(" > 50, add a separate 'travel cost' line item. and travel cost should not be more that 30% of total cost of the invoice and travel cost should be atleast $25.\n");
        systemPrompt.append("4. Ensure quantity for each line item should be a whole number  \n");
        systemPrompt.append("5. Ensure that you do not change the rate of any item, just adjust the quantity accordingly. \n");
        systemPrompt.append("7. if total of invoice is not coming up exactly as invoice ");
                       systemPrompt.append(amount);
                       systemPrompt.append( "which I am giving you as  ");
                                                      systemPrompt.append(amount);
                                                      systemPrompt.append( " and less than it then adjust travel line item by increasing the quantity \n");
        systemPrompt.append("8. Ensure the sum of all line items's (rate * quantity) including 'travel cost' equals to invoice");
                                                                      systemPrompt.append(amount);
                                                                      systemPrompt.append( "  which I am giving you \n");
        systemPrompt.append("9. If data is missing, leave fields blank.\n");

        StringBuilder itemList = new StringBuilder("Available items:\n");
        for (ZohoItem it : items) {
            itemList.append("- item_id: ").append(it.getItem_id())
                    .append(", name: ").append(it.getName())
                    .append(", rate: ").append(it.getRate())
                    .append(", active: ").append(it.isActive())
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
    }*/


}
