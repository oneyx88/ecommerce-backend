package com.commerce.order.service;

import com.commerce.order.dto.CreateOrderRequest;
import com.commerce.order.dto.OrderItemResponse;
import com.commerce.order.dto.OrderResponse;
import com.commerce.order.dto.PagedOrderResponse;
import com.commerce.order.kafka.event.PaymentExpiredEvent;
import com.commerce.order.kafka.event.PaymentSucceededEvent;

import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/11/2 23:07
 * @package com.commerce.order.service
 * <p>
 * Description:
 */
public interface OrderService {
    OrderResponse createOrder(String keycloakId, String userEmail, CreateOrderRequest request);

    void updatePaymentId(Long orderId, Long paymentId);

    void markOrderAsPaid(PaymentSucceededEvent event);

    void markOrderAsExpired(PaymentExpiredEvent event);

    OrderResponse getOrderById(Long orderId);

    List<OrderResponse> getOrderByUser(String keycloakId);

    Long getOrderCount();

    void cancelOrderByAdmin(Long orderId);

    PagedOrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    List<OrderItemResponse> getSellerOrders(String sellerId);
}
