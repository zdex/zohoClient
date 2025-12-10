package com.zoho.client.model;

public class ZohoItem {
    private String item_id;
    private String name;
    private String description;
    private Double rate;

    public String getItem_id() { return item_id; }
    public void setItem_id(String item_id) { this.item_id = item_id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }
}
