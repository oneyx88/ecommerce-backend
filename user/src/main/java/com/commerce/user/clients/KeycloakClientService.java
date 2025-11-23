package com.commerce.user.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakClientService {
    private final KeycloakFeignClient feignClient;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    public KeycloakClientService(KeycloakFeignClient feignClient) {
        this.feignClient = feignClient;
    }

    public KeycloakTokenResponse getPasswordToken(String username, String password) {
        Map<String, Object> form = new HashMap<>();
        form.put("grant_type", "password");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("username", username);
        form.put("password", password);
        return feignClient.passwordToken(form);
    }

    public KeycloakTokenResponse refreshToken(String refreshToken) {
        Map<String, Object> form = new HashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("refresh_token", refreshToken);
        return feignClient.refreshToken(form);
    }

    public void logout(String refreshToken) {
        Map<String, Object> form = new HashMap<>();
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("refresh_token", refreshToken);
        feignClient.logout(form);
    }
}