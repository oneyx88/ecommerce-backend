package com.commerce.inventory.client;

import com.commerce.inventory.dto.ProductResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/11/2 23:38
 * @package com.commerce.order.service
 * <p>
 * Description:
 */
@Service
public class ProductClientService {
    @Autowired
    private ProductFeignClient productFeignClient;

    public List<ProductResponse> getSellerProducts(String sellerId){
        return productFeignClient.getAllSellerProducts(sellerId);
    }
}
