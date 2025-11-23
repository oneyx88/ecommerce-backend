package com.commerce.user.config;

 import org.modelmapper.ModelMapper;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;
 import org.springframework.web.client.RestTemplate;

/**
 * @author Yixi Wan
 * @date 2025/10/21 20:03
 * @package com.commerce.ecommapp.config
 * <p>
 * Description:
 */
@Configuration
class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
