package com.commerce.order.repository;

import com.commerce.order.model.Order;
import com.commerce.order.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/11/2 23:08
 * @package com.commerce.order.repository
 * <p>
 * Description:
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByKeycloakId(String keycloakId);

    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o WHERE oi.productId IN (:productIds)")
    List<OrderItem> findOrderItemsByProductIds(@Param("productIds") List<Long> productIds);
}
