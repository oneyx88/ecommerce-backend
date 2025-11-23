package com.commerce.order.controller;

import com.commerce.order.config.AppConstants;
import com.commerce.order.dto.CreateOrderRequest;
import com.commerce.order.dto.OrderItemResponse;
import com.commerce.order.dto.OrderResponse;
import com.commerce.order.dto.PagedOrderResponse;
import com.commerce.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * @author Yixi Wan
 * @date 2025/11/2 23:07
 * @package com.commerce.order.controller
 * <p>
 * Description:
 */
@RestController
@RequestMapping("/api/v1/orders")
class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<OrderResponse> createOrder(
            @RequestHeader("X-User-Id") String keycloakId,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestBody CreateOrderRequest request
    ) {
        OrderResponse orderResponse = orderService.createOrder(keycloakId, userEmail, request);

        return ResponseEntity
                .created(URI.create("/api/v1/orders/" + orderResponse.getOrderId()))
                .body(orderResponse);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<OrderResponse>> getOrderByUser(@RequestHeader("X-User-Id") String keycloakId) {
        List<OrderResponse> orderResponse = orderService.getOrderByUser(keycloakId);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getOrderCount() {
        return ResponseEntity.ok(orderService.getOrderCount());
    }

    @PutMapping("/{orderId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrderByAdmin(orderId);
        return ResponseEntity.ok(new String("Order cancelled by admin"));
    }


    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedOrderResponse> getAllOrders(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false) Integer pageNumber,
            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_ORDERS_BY, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false) String sortOrder
    ) {
        PagedOrderResponse response = orderService.getAllOrders(pageNumber, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<OrderItemResponse>> getSellerOrders(
            @RequestHeader("X-User-Id") String sellerId
    ) {
        List<OrderItemResponse> response = orderService.getSellerOrders(sellerId);
        return ResponseEntity.ok(response);
    }







}
