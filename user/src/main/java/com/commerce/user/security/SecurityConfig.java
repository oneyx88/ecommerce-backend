package com.commerce.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Yixi Wan
 * @date 2025/11/6 12:02
 * @package com.commerce.user.security
 * <p>
 * Description:
 */
@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（如果是 REST API）
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 注册接口放行
                        .requestMatchers("/api/v1/users/signup","/api/v1/users/signup/**", "/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/login", "/api/v1/users/logout", "/api/v1/users/refresh").permitAll()
                        // 其他接口需要验证
                        .anyRequest().authenticated()
                )

                // ✅ 关键：让 Resource Server 只对被保护的接口启用 JWT 过滤
                .oauth2ResourceServer(oauth2 ->
                        oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    /**
     * ✅ 自定义角色解析器：从 Keycloak 的 realm_access.roles 中提取角色
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    /**
     * 🧩 手动提取 Keycloak 的 roles
     */
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof Collection<?> realmRoles) {
            realmRoles.forEach(roleObj -> roles.add(roleObj.toString()));
        }

        // 打印出解析结果
        log.info("JWT realm_access: {}", realmAccess);
        log.info("Extracted raw roles: {}", roles);

        // 统一加 ROLE_ 前缀（避免重复）
        Set<GrantedAuthority> authorities = roles.stream()
                .map(role -> "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());

        log.info("Mapped authorities: {}", authorities);

        return authorities;
    }
}
