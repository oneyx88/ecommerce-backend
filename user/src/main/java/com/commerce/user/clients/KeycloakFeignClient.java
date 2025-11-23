package com.commerce.user.clients;

import feign.Headers;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Map;

/**
 * Keycloak Token & Logout 调用
 */
@FeignClient(
        name = "keycloak-feign",
        url = "${keycloak.auth-server-url}",
        path = "/realms/${keycloak.realm}/protocol/openid-connect",
        configuration = KeycloakFeignConfig.class
)
public interface KeycloakFeignClient {

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Headers("Content-Type: application/x-www-form-urlencoded")
    KeycloakTokenResponse passwordToken(Map<String, ?> form);

    @PostMapping(value = "/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Headers("Content-Type: application/x-www-form-urlencoded")
    KeycloakTokenResponse refreshToken(Map<String, ?> form);

    @PostMapping(value = "/logout", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Headers("Content-Type: application/x-www-form-urlencoded")
    void logout(Map<String, ?> form);
}