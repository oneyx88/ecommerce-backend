package com.commerce.order.clients;

import com.commerce.order.dto.AddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author Yixi Wan
 * @date 2025/11/19 14:23
 * @package com.commerce.order.clients
 * <p>
 * Description:
 */
@FeignClient(name = "address-service", path = "/api/v1/addresses")
public interface AddressFeignClient {
    @GetMapping("/{addressId}")
    AddressResponse getAddressById(@PathVariable Long addressId);
}
