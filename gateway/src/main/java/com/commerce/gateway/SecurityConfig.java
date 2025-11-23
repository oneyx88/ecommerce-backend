package com.commerce.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yixi Wan
 * @date 2025/11/5 20:14
 * @package com.commerce.gateway
 * <p>
 * Description:
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                        ServerAuthenticationEntryPoint authEntryPoint,
                                                        ServerAccessDeniedHandler accessDeniedHandler) {

        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // 启用 CORS 支持（使用下方 CorsWebFilter 配置）
                .cors(Customizer.withDefaults())

                .authorizeExchange(exchanges -> exchanges
                        // ✅ 公共接口（无需 Token）
                        .pathMatchers(
                                "/actuator/**",
                                "/login/**",
                                "/oauth2/**",
                                "/users/signup",
                                "/users/signup/**",
                                "/users/login",
                                "/users/logout",
                                "/users/refresh",
                                "/fallback/**",
                                "/api/v1/payments/stripe/webhook/**"
                        ).permitAll()

                        // ✅ 放行所有预检请求（CORS 需要）
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ 放行产品与分类的所有 GET 请求
                        .pathMatchers(HttpMethod.GET, "/products/**","/products", "/categories/**", "/categories",  "/images/**").permitAll()

                        // ✅ 其他所有请求：只要有合法 JWT 就放行
                        .anyExchange().authenticated()
                )

                // ✅ Gateway 只验证 JWT，不解析角色权限
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                // ✅ 自定义 401/403 响应为统一 JSON
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // ✅ 简单打印整个 JWT payload（所有 claims）
            System.out.println(">>> JWT Claims = " + jwt.getClaims());

            // 正常角色提取逻辑（可保留或暂时注释）
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            List<String> roles = Collections.emptyList();

            if (realmAccess != null && realmAccess.get("roles") instanceof List<?>) {
                roles = ((List<?>) realmAccess.get("roles")).stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .toList();
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            return Flux.fromIterable(authorities);
        });

        return converter;
    }

    /**
     * 全局 CORS 过滤器：支持跨域、预检请求与凭证
     * 可通过环境变量 ALLOWED_ORIGINS 配置允许来源（逗号分隔），默认 http://localhost:5173
     */
    @Bean
    public CorsWebFilter corsWebFilter(Environment env) {
        String originsStr = env.getProperty("ALLOWED_ORIGINS", "http://localhost:5173");
        List<String> allowedOrigins = Arrays.stream(originsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList("Authorization", "Link", "X-Total-Count"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }


    /**
     * 限流用：按 IP 做 key
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
                exchange.getRequest()
                        .getHeaders()
                        .getFirst("X-Forwarded-For") != null
                        ? exchange.getRequest().getHeaders().getFirst("X-Forwarded-For")
                        : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint(ObjectMapper mapper, Environment env) {
        return (exchange, ex) -> {
            // 预检请求直接放行（避免 401 影响 CORS）
            if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
                applyCorsHeaders(exchange, exchange.getResponse(), env);
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                return exchange.getResponse().setComplete();
            }

            String message = Optional.ofNullable(ex.getMessage())
                    .map(msg -> msg.toLowerCase().contains("expired") ? "Token expired" : "Unauthorized")
                    .orElse("Unauthorized");

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", message);
            body.put("code", "AUTH_401");
            body.put("status", 401);
            body.put("timestamp", Instant.now().toString());

            byte[] bytes;
            try {
                bytes = mapper.writeValueAsBytes(body);
            } catch (Exception e) {
                bytes = ("{\"message\":\"Unauthorized\",\"code\":\"AUTH_401\",\"status\":401}").getBytes();
            }

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            applyCorsHeaders(exchange, response, env);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        };
    }

    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler(ObjectMapper mapper, Environment env) {
        return (exchange, ex) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("message", "Access denied");
            body.put("code", "AUTH_403");
            body.put("status", 403);
            body.put("timestamp", Instant.now().toString());

            byte[] bytes;
            try {
                bytes = mapper.writeValueAsBytes(body);
            } catch (Exception e) {
                bytes = ("{\"message\":\"Access denied\",\"code\":\"AUTH_403\",\"status\":403}").getBytes();
            }

            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.FORBIDDEN);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            applyCorsHeaders(exchange, response, env);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        };
    }

    private void applyCorsHeaders(ServerWebExchange exchange, ServerHttpResponse response, Environment env) {
        String originsStr = env.getProperty("ALLOWED_ORIGINS", "http://localhost:5173");
        List<String> allowedOrigins = Arrays.stream(originsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        String origin = exchange.getRequest().getHeaders().getFirst("Origin");
        if (origin != null && allowedOrigins.contains(origin)) {
            response.getHeaders().set("Access-Control-Allow-Origin", origin);
            response.getHeaders().set("Access-Control-Allow-Credentials", "true");
            // 反射请求头，尽可能满足浏览器校验
            String reqHeaders = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("Access-Control-Request-Headers")).orElse("*");
            response.getHeaders().set("Access-Control-Allow-Headers", reqHeaders);
            response.getHeaders().set("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,PATCH,OPTIONS");
            response.getHeaders().set("Access-Control-Expose-Headers", "WWW-Authenticate, Authorization, Link, X-Total-Count");
        }
    }

    /**
     * 在 Security 链之前运行的 WebFilter：
     * 若请求未携带 Authorization，则从 access_token Cookie 注入为 Bearer Token。
     */
    @Bean
    public WebFilter cookieAuthHeaderWebFilter() {
        return new CookieAuthHeaderWebFilter();
    }

    static class CookieAuthHeaderWebFilter implements WebFilter, Ordered {
        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE; // 尽可能早，确保 Security 能读取到
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || authHeader.isBlank()) {
                var cookie = exchange.getRequest().getCookies().getFirst("access_token");
                if (cookie != null) {
                    String token = cookie.getValue();
                    if (token != null && !token.isBlank()) {
                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("Authorization", "Bearer " + token)
                                .build();
                        ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
                        return chain.filter(mutatedExchange);
                    }
                }
            }
            return chain.filter(exchange);
        }
    }
}
