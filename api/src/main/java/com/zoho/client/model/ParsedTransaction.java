package com.zoho.client.model;

import java.time.LocalDate;

public class ParsedTransaction {
    public int row;
    public LocalDate date;
    public double amount;
    public String description;
    public String customerName;
    public String referenceNumber;
    public boolean duplicate;

    // getters/setters omitted for brevity


    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public boolean isDuplicate() {
        return duplicate;
    }

    public void setDuplicate(boolean duplicate) {
        this.duplicate = duplicate;
    }
}
