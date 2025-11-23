package com.commerce.order.clients;

import com.commerce.order.dto.ProductDTO;
import com.commerce.order.dto.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/10/29 16:19
 * @package com.commerce.order.service
 * <p>
 * Description:
 */
@FeignClient(name = "product-service", path = "/api/v1/products")
public interface ProductFeignClient {

    @GetMapping("/{productId}")
    ProductDTO getProductById(@PathVariable Long productId);

    @GetMapping("/sellers/{sellerId}")
    List<ProductResponse> getAllSellerProducts(@PathVariable String sellerId);
}