package com.commerce.user.clients;

import feign.codec.Encoder;
import feign.form.FormEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Yixi Wan
 * @date 2025/11/18 20:12
 * @package com.commerce.user.clients
 * <p>
 * Description:
 */
@Configuration
public class KeycloakFeignConfig {

    @Bean
    public Encoder feignFormEncoder() {
        return new FormEncoder();
    }
}