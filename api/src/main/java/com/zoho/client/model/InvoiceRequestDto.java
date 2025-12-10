// src/main/java/com/zoho/client/dto/InvoiceRequestDto.java
package com.zoho.client.model;

import java.time.LocalDate;

public class InvoiceRequestDto {

    private String customerName;          // Extracted from transaction
    private String transactionReference;  // Bank/Zelle reference
    private String description;           // Transaction description
    private Double amount;                // Total invoice amount
    private LocalDate date;               // Invoice date

    // getters & setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
