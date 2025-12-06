package com.zoho.client.controller;

import com.zoho.client.model.ZohoTokenResponse;
import com.zoho.client.service.ZohoAuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController // adjust as needed
public class AuthController {

    private final ZohoAuthService authService;

    public AuthController(ZohoAuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/auth/url")
    public String getAuthUrl() {
        String authUrl = authService.buildAuthorizationUrl();
        return authUrl;
    }

    @PostMapping("/auth/exchange")
    public ZohoTokenResponse exchangeCode(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        return authService.exchangeCodeForToken(code);
    }

    @GetMapping("/auth/token")
    public ZohoTokenResponse getCurrentToken() {
        return authService.getCurrentTokenSnapshot();
    }
}
