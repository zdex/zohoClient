package com.zoho.client.cache;

import com.zoho.client.model.ParsedTransaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class PreviewCache {

    private final AtomicReference<List<ParsedTransaction>> cache =
            new AtomicReference<>();

    public void store(List<ParsedTransaction> transactions) {
        cache.set(transactions);
    }

    public List<ParsedTransaction> get() {
        List<ParsedTransaction> data = cache.get();
        if (data == null) {
            throw new IllegalStateException("No preview data found. Upload file first.");
        }
        return data;
    }

    public void clear() {
        cache.set(null);
    }
}
