package com.commerce.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * @author Yixi Wan
 * @date 2025/11/16 17:02
 * @package com.commerce.product.config
 * <p>
 * Description:
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Value("${file.path}")
    private String path;

    @Value("${user.dir}")
    private String userDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String basePath = path;
        String location;
        if (new File(basePath).isAbsolute()) {
            location = "file:" + basePath;
        } else {
            location = "file:" + userDir + File.separator + basePath;
        }
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        registry.addResourceHandler("/images/**").addResourceLocations(location);
    }
}
