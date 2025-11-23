package com.commerce.user.service;

import com.commerce.user.clients.KeycloakClientService;
import com.commerce.user.clients.KeycloakTokenResponse;
import com.commerce.user.dto.auth.LoginRequest;
import com.commerce.user.dto.auth.LoginSuccessResponse;
import com.commerce.user.dto.auth.LoginUserResponse;
import com.commerce.user.dto.auth.RefreshTokenResponse;
import com.commerce.user.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yixi Wan
 * @date 2025/11/19
 * @package com.commerce.user.service
 * <p>
 * Description: 认证服务实现，负责调用 Keycloak token 与注销接口
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Value("${keycloak.auth-server-url}")
    private String keycloakServerUrl;
    @Value("${keycloak.realm}")
    private String realm;
    @Value("${keycloak.resource}")
    private String clientId;
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Autowired
    private KeycloakClientService keycloakClientService;


    private String tokenEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/token", trimTrailingSlash(keycloakServerUrl), realm);
    }

    private String logoutEndpoint() {
        return String.format("%s/realms/%s/protocol/openid-connect/logout", trimTrailingSlash(keycloakServerUrl), realm);
    }

    private String trimTrailingSlash(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

//    @Override
//    public LoginSuccessResponse login(LoginRequest request) {
//        KeycloakTokenResponse token = keycloakClientService.getPasswordToken(request.getUsername(), request.getPassword());
//        if (token == null || token.getAccessToken() == null) {
//            throw new ApiException("Keycloak 登录失败", HttpStatus.UNAUTHORIZED);
//        }
//        Map<String, Object> claims = parseJwtClaims(token.getAccessToken());
//        String sub = (String) claims.getOrDefault("sub", "");
//        LoginUserResponse userInfo = LoginUserResponse.builder()
//                .id(sub)
//                .email((String) claims.getOrDefault("email", ""))
//                .firstName((String) claims.getOrDefault("given_name", ""))
//                .lastName((String) claims.getOrDefault("family_name", ""))
//                .build();
//
//        return LoginSuccessResponse.builder()
//                .message("Login successful")
//                .user(userInfo)
//                .accessTokenExpiresIn(token.getExpiresIn())
//                .refreshTokenExpiresIn(token.getRefreshExpiresIn())
//                .build();
//    }

    @Override
    public KeycloakTokenResponse loginForToken(LoginRequest request) {
        KeycloakTokenResponse token = keycloakClientService.getPasswordToken(request.getUsername(), request.getPassword());
        if (token == null || token.getAccessToken() == null) {
            throw new ApiException("Keycloak 登录失败", HttpStatus.UNAUTHORIZED);
        }
        return token;
    }

    @Override
    public RefreshTokenResponse refresh(String refreshToken) {
        KeycloakTokenResponse token = keycloakClientService.refreshToken(refreshToken);
        if (token == null || token.getAccessToken() == null) {
            throw new ApiException("刷新 token 失败", HttpStatus.UNAUTHORIZED);
        }
        return RefreshTokenResponse.builder()
                .message("Token refreshed")
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .accessTokenExpiresIn(token.getExpiresIn())
                .refreshTokenExpiresIn(token.getRefreshExpiresIn())
                .build();
    }

    @Override
    public LoginSuccessResponse loginAndSetCookie(LoginRequest request, HttpServletResponse response) {
        // 1️⃣ 获取 token
        KeycloakTokenResponse token = keycloakClientService.getPasswordToken(request.getUsername(), request.getPassword());
        if (token == null || token.getAccessToken() == null) {
            throw new ApiException("Keycloak 登录失败", HttpStatus.UNAUTHORIZED);
        }

        // 2️⃣ refresh_token Cookie
        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", token.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(token.getRefreshExpiresIn() != null ? token.getRefreshExpiresIn() : 7 * 24 * 3600)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 3️⃣ access_token Cookie
        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", token.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(token.getExpiresIn() != null ? token.getExpiresIn() : 300)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        // 4️⃣ 解析 JWT
        Map<String, Object> claims = parseJwtClaims(token.getAccessToken());

        // 5️⃣ 提取 realm roles
        List<String> allRoles = extractRealmRoles(claims);

        // 6️⃣ 筛选业务角色（ADMIN / USER / SELLER）
        Set<String> businessRoles = filterBusinessRoles(allRoles);

        // 7️⃣ 构建 user info
        LoginUserResponse userInfo = LoginUserResponse.builder()
                .id((String) claims.getOrDefault("sub", ""))
                .email((String) claims.getOrDefault("email", ""))
                .firstName((String) claims.getOrDefault("given_name", ""))
                .lastName((String) claims.getOrDefault("family_name", ""))
                .roles(businessRoles)     // <<<<< 返回 Set<String>
                .build();

        // 8️⃣ 构建响应
        return LoginSuccessResponse.builder()
                .message("Login successful")
                .user(userInfo)
                .accessTokenExpiresIn(token.getExpiresIn())
                .refreshTokenExpiresIn(token.getRefreshExpiresIn())
                .build();
    }


    @Override
    public RefreshTokenResponse refreshAndRotateCookie(String refreshToken, HttpServletResponse response) {
        KeycloakTokenResponse token = keycloakClientService.refreshToken(refreshToken);
        if (token == null || token.getAccessToken() == null) {
            throw new ApiException("刷新 token 失败", HttpStatus.UNAUTHORIZED);
        }

        // 旋转 refresh_token Cookie
        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", token.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(token.getRefreshExpiresIn() != null ? token.getRefreshExpiresIn() : 7 * 24 * 3600)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 旋转 access_token Cookie
        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", token.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(token.getExpiresIn() != null ? token.getExpiresIn() : 300)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return RefreshTokenResponse.builder()
                .message("Token refreshed")
                .accessToken(token.getAccessToken())
                .refreshToken(token.getRefreshToken())
                .accessTokenExpiresIn(token.getExpiresIn())
                .refreshTokenExpiresIn(token.getRefreshExpiresIn())
                .build();
    }

    @Override
    public void logoutAndClearCookie(String refreshToken, HttpServletResponse response) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            logout(refreshToken);
        }

        // 清理 refresh_token Cookie
        ResponseCookie refreshCookie = ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        // 清理 access_token Cookie
        ResponseCookie accessCookie = ResponseCookie
                .from("access_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    }

    @Override
    public void logout(String refreshToken) {
        try {
            keycloakClientService.logout(refreshToken);
        } catch (Exception e) {
            throw new ApiException("Keycloak 注销失败", HttpStatus.BAD_REQUEST);
        }
    }

    private static String encode(String s) {
        return s == null ? "" : UriUtils.encode(s, StandardCharsets.UTF_8);
    }

    private static Map<String, Object> parseJwtClaims(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return Collections.emptyMap();
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(payloadJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRealmRoles(Map<String, Object> claims) {
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        if (realmAccess == null) return List.of();

        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List<?>) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    private String determineFinalRole(List<String> roles) {
        if (roles.contains("ADMIN")) return "ADMIN";
        if (roles.contains("USER")) return "USER";
        if (roles.contains("SELLER")) return "SELLER";

        throw new ApiException("用户缺少有效角色", HttpStatus.FORBIDDEN);
    }

    private Set<String> filterBusinessRoles(List<String> roles) {
        Set<String> allowed = Set.of("ADMIN", "USER", "SELLER");

        Set<String> result = roles.stream()
                .filter(allowed::contains)
                .collect(Collectors.toSet());

        if (result.isEmpty()) {
            throw new ApiException("用户缺少有效角色", HttpStatus.FORBIDDEN);
        }

        return result;
    }


}