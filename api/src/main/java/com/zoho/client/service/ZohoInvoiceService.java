// src/main/java/com/zoho/client/service/ZohoInvoiceService.java
package com.zoho.client.service;

import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.InvoiceRequestDto;
import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ZohoInvoiceService {

    private final RestClient restClient;
    private final ZohoProperties props;
    private final ZohoAuthService authService;
    private final ZohoContactsService contactService;
    private final LineItemAiService aiService;
    private final ObjectMapper mapper = new ObjectMapper();

    public ZohoInvoiceService(RestClient.Builder builder,
                              ZohoProperties props,
                              ZohoAuthService authService,
                              ZohoContactsService contactService,
                              LineItemAiService aiService) {
        this.restClient = builder.build();
        this.props = props;
        this.authService = authService;
        this.contactService = contactService;
        this.aiService = aiService;
    }

    public JsonNode createInvoiceWithPayment(InvoiceRequestDto dto) throws Exception {

        ZohoTokenResponse token = authService.getValidToken();

        // 1. Get or create contact
        String contactId = contactService.getOrCreateContactId(dto.getCustomerName());

        // 2. Ask OpenAI to generate line_items
        JsonNode lineItemsNode = aiService.generateLineItems(dto.getAmount());
        JsonNode lineItemsArray = lineItemsNode.get("line_items");

        // 3. Create invoice in Zoho
        String createInvoiceUrl = props.getBooksBaseUrl()
                + "/invoices?organization_id=" + props.getOrganizationId();

        Map<String, Object> invoiceBody = new HashMap<>();
        invoiceBody.put("customer_id", contactId);
        invoiceBody.put("reference_number", dto.getTransactionReference());
        invoiceBody.put("date", dto.getDate().format(DateTimeFormatter.ISO_DATE));
        invoiceBody.put("due_date", dto.getDate().plusDays(14).format(DateTimeFormatter.ISO_DATE));
        invoiceBody.put("line_items", mapper.convertValue(lineItemsArray, Object.class));
        invoiceBody.put("payment_terms", 0);
        invoiceBody.put("notes", dto.getDescription());

        String invoiceRespStr = restClient.post()
                .uri(createInvoiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .body(invoiceBody)
                .retrieve()
                .body(String.class);

        JsonNode invoiceRoot = mapper.readTree(invoiceRespStr);
        JsonNode invoiceNode = invoiceRoot.path("invoice");
        String invoiceId = invoiceNode.path("invoice_id").asText();

        // 4. Mark invoice as Sent
        String markSentUrl = props.getBooksBaseUrl()
                + "/invoices/" + invoiceId + "/status/sent?organization_id=" + props.getOrganizationId();

        restClient.post()
                .uri(markSentUrl)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .toBodilessEntity();

        // 5. Create payment
        String paymentUrl = props.getBooksBaseUrl()
                + "/customerpayments?organization_id=" + props.getOrganizationId();

        Map<String, Object> paymentBody = new HashMap<>();
        paymentBody.put("customer_id", contactId);
        paymentBody.put("invoice_id", invoiceId);
        paymentBody.put("amount", dto.getAmount());
        paymentBody.put("payment_mode", "Wells Zelle");
        paymentBody.put("payment_status", "paid");
        paymentBody.put("account_name", "[ Wellsfargo ] Wellsfargo busniness account");
        paymentBody.put("account_type", "bank");
        paymentBody.put("reference_number", dto.getTransactionReference());
        paymentBody.put("date", dto.getDate().format(DateTimeFormatter.ISO_DATE));

        Map<String, Object> invoiceEntry = new HashMap<>();
        invoiceEntry.put("invoice_id", invoiceId);
        invoiceEntry.put("amount_applied", dto.getAmount());

        paymentBody.put("invoices", List.of(invoiceEntry));

        String paymentRespStr = restClient.post()
                .uri(paymentUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .body(paymentBody)
                .retrieve()
                .body(String.class);

        JsonNode paymentRoot = mapper.readTree(paymentRespStr);
// Validate Zoho response
        int paymentCode = paymentRoot.path("code").asInt(-1);
        String paymentMessage = paymentRoot.path("message").asText();

        if (paymentCode != 0) {
            throw new RuntimeException("Zoho payment creation failed: " + paymentMessage +
                    " | Response: " + paymentRespStr);
        }

// Extract the payment object returned by Zoho
        JsonNode paymentNode = paymentRoot.path("payment");

// Build combined result
        Map<String, Object> combined = new HashMap<>();
        combined.put("invoice", invoiceNode);
        combined.put("payment", paymentNode);
        combined.put("payment_message", paymentMessage);
        combined.put("payment_code", paymentCode);

        // 6. Create invoice in Zoho
        String updateInvoiceUrl = props.getBooksBaseUrl()
                + "/invoices/" + invoiceId +"?organization_id=" + props.getOrganizationId();

        Map<String, Object> updateInvoiceBody = new HashMap<>();
        updateInvoiceBody.put("status", "paid");
        updateInvoiceBody.put("status_formatted", "Paid");

        String updateIinvoiceRespStr = restClient.put()
                .uri(updateInvoiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .body(updateInvoiceBody)
                .retrieve()
                .body(String.class);

        JsonNode updateIinvoiceRoot = mapper.readTree(updateIinvoiceRespStr);
        int updateIinvoiceCode = updateIinvoiceRoot.path("code").asInt(-1);
        String message = updateIinvoiceRoot.path("message").asText();

        if (updateIinvoiceCode != 0) {
            throw new RuntimeException("Zoho payment creation failed: " + paymentMessage +
                    " | Response: " + paymentRespStr);
        }

        return mapper.valueToTree(combined);
    }
}
