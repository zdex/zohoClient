// src/main/java/com/zoho/client/service/ZohoInvoiceService.java
package com.zoho.client.service;

import com.zoho.client.config.ZohoProperties;
import com.zoho.client.model.InvoiceRequestDto;
import com.zoho.client.model.ParsedTransaction;
import com.zoho.client.model.ZohoTokenResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
    private String cachedExpenseAccountId;

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
    public boolean invoiceExistsByReference(String reference) throws Exception {
        JsonNode root = getInvoicesByReferenceNumber(reference, 1, 1);
        return root.path("invoices").size() > 0;
    }
    public JsonNode createInvoiceWithPayment(ParsedTransaction dto) throws Exception {
        if(this.invoiceExistsByReference(dto.getTransactionReference())) {
            return null;
        }
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
        JsonNode lineItems = invoiceNode.path("line_items");
        String invoiceId = invoiceNode.path("invoice_id").asText();
        double miles = 0;
        double mileageRate = 0.65;

        for (JsonNode item : invoiceNode.path("line_items")) {
            if (item.path("name").asText().toLowerCase().contains("travel")) {
                miles = item.path("quantity").asDouble();
              //  mileageRate = item.path("rate").asDouble(0.65);
            }
        }
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
        paymentBody.put("payment_status_formatted", "Paid");
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

        // 6. update invoice in Zoho with paid status
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

        //7. create expense for invoice
        JsonNode expenseNode = createExpenseForInvoice(
                invoiceId,
                dto.getDate(),
                contactId,
                dto.getTransactionReference(),
                miles,
                mileageRate
        );

        combined.put("expense", expenseNode);
        return mapper.valueToTree(combined);
    }

    public JsonNode createExpenseForInvoice(String invoiceId,
                                            LocalDate date,
                                            String customerId,
                                            String referenceNumber,
                                            double miles,
                                            double mileageRate) throws Exception {
      //  miles = 100d;
        ZohoTokenResponse token = authService.getValidToken();
        if (miles <= 0) {
            System.out.println("No Travel Cost line item found → skipping expense creation.");
            return null;
        }
        // Get account_id (cached after first lookup)
        String accountId = getExpenseAccountId();

        String expenseUrl = props.getBooksBaseUrl()
                + "/expenses?organization_id=" + props.getOrganizationId();

        Map<String, Object> expenseBody = new HashMap<>();
        expenseBody.put("expense_type", "manual");
        expenseBody.put("date", date.format(DateTimeFormatter.ISO_DATE));
        expenseBody.put("employee_id", props.getMileageEmployeeId());
        expenseBody.put("category_id", "4991661000000084140");
        expenseBody.put("account_id", accountId);
        expenseBody.put("distance", miles);
        expenseBody.put("mileage_rate", mileageRate);
        expenseBody.put("vehicle_id", "4991661000000105001");
        expenseBody.put("customer_id", customerId);
        expenseBody.put("reference_number", referenceNumber);
        expenseBody.put("notes", "Mileage expense auto-created for invoice " + invoiceId);
        System.out.println(mapper.writeValueAsString(expenseBody));
        String respStr = restClient.post()
                .uri(expenseUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .body(expenseBody)
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(respStr);

        if (root.path("code").asInt() != 0) {
            throw new RuntimeException("Zoho Expense creation failed: " + root);
        }

        return root.path("expense");
    }
    public JsonNode getBankAccounts() throws Exception {
        ZohoTokenResponse token = authService.getValidToken();
        String url = props.getBooksBaseUrl()
                + "/bankaccounts?organization_id=" + props.getOrganizationId();

        String resp = restClient.get()
                .uri(url)
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(resp);

        if (root.path("code").asInt() != 0) {
            throw new RuntimeException("Unable to fetch bank accounts: " + resp);
        }

        return root.path("bankaccounts");
    }


    private String getExpenseAccountId() throws Exception {

        // Return cached ID if already loaded
        if (cachedExpenseAccountId != null) {
            return cachedExpenseAccountId;
        }

        JsonNode accounts = getBankAccounts();

        // Step 1: Try to find Corporate Account
        for (JsonNode acc : accounts) {
            if (acc.path("account_name").asText().contains("Wellsfargo busniness account")) {
                cachedExpenseAccountId = acc.path("account_id").asText();
                return cachedExpenseAccountId;
            }
        }

        // Step 2: Fallback — pick first active bank account
        for (JsonNode acc : accounts) {
            if (acc.path("is_active").asBoolean(true)) {
                cachedExpenseAccountId = acc.path("account_id").asText();
                return cachedExpenseAccountId;
            }
        }

        throw new RuntimeException("No valid bank account found for expenses!");
    }

    public JsonNode getInvoicesByReferenceNumber(
            String referenceNumber,
            int page,
            int perPage
    ) throws Exception {
        ZohoTokenResponse token = authService.getValidToken();
        String url = props.getBooksBaseUrl()
                + "/invoices"
                + "?organization_id=" + props.getOrganizationId()
                + "&reference_number=" + URLEncoder.encode(referenceNumber, StandardCharsets.UTF_8)
                + "&page=" + page
                + "&per_page=" + perPage;

        String response = restClient.get()
                .uri(url)   // ✅ FULL URL, no uriBuilder
                .header("Authorization", "Zoho-oauthtoken " + token.getAccess_token())
                .retrieve()
                .body(String.class);

        JsonNode root = mapper.readTree(response);

        if (root.path("code").asInt() != 0) {
            throw new RuntimeException("Zoho invoice search failed: " + response);
        }

        return root;
    }


}
