package com.commerce.product.kafka.consumer;

import com.commerce.product.clients.InventoryClientService;
import com.commerce.product.clients.InventoryResponse;
import com.commerce.product.dto.product.ProductResponse;
import com.commerce.product.kafka.event.InventoryEvent;
import com.commerce.product.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * @author Yixi Wan
 * @date 2025/11/3 14:16
 * @package com.commerce.product.kafka
 * <p>
 * Description:
 */
@Slf4j
@Component
class InventoryEventConsumer {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryClientService inventoryClientService;
    @Autowired
    private ModelMapper modelMapper;

    /**
     * 监听库存变更事件（inventory-stock-updated）
     */
    @KafkaListener(topics = "inventory-stock-updated", groupId = "product-cache-group")
    public void handleInventoryEvent(InventoryEvent event) {
        Long productId = event.getProductId();
        String eventType = event.getEventType();
        String cacheKey = "product_cache:" + productId;

        log.info("📩 Received inventory event: productId={}, eventType={}, time={}",
                productId, eventType, event.getEventTime());

        switch (eventType) {
            case "CREATE":
            case "UPDATE":
            case "LOCK":
            case "RELEASE":
            case "CONFIRM":
                productRepository.findById(productId).ifPresentOrElse(product -> {
                    // 映射为 DTO
                    ProductResponse response = modelMapper.map(product, ProductResponse.class);

                    try {
                        // 调用 Feign 客户端获取库存数据
                        InventoryResponse inventory = inventoryClientService.getInventoryByProductId(productId);
                        response.setAvailableStock(inventory.getAvailableStock());
                    } catch (Exception e) {
                        log.warn("Failed to fetch inventory for productId={}, reason={}", productId, e.getMessage());
                    }

                    // ✅ 更新缓存
                    redisTemplate.opsForValue().set(cacheKey, response);
                    log.info("Refreshed product cache for productId={} after event={}", productId, eventType);

                }, () -> log.warn("Product not found for productId={}, skipping cache update.", productId));
                break;

            case "DELETE":
                redisTemplate.delete(cacheKey);
                log.info("🗑️ Deleted product cache for productId={} due to DELETE event.", productId);
                break;

            default:
                log.warn("⚠️ Unknown inventory eventType='{}' for productId={}", eventType, productId);
                break;
        }
    }




}
