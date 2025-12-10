package com.zoho.client.config;

import com.zoho.client.service.ZohoItemCacheService;
import com.zoho.client.service.ZohoOrganizationService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

//@Component
public class StartupInitializer {

    private final ZohoOrganizationService orgService;
    private final ZohoItemCacheService itemCache;

    public StartupInitializer(ZohoOrganizationService orgService,
                              ZohoItemCacheService itemCache) {
        this.orgService = orgService;
        this.itemCache = itemCache;
    }

    //@PostConstruct
    public void init() {
        try {
            orgService.getOrganizationId();   // loads & caches orgId
            itemCache.loadItems();           // loads items AFTER orgId is known
            System.out.println("🚀 Initialization complete");
        }
        catch (Exception e) {
            System.err.println("Error initializing Zoho: " + e.getMessage());
        }
    }
}
