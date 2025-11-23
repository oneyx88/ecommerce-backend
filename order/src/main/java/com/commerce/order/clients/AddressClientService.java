package com.commerce.order.clients;

import com.commerce.order.dto.AddressResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Yixi Wan
 * @date 2025/11/19 14:27
 * @package com.commerce.order.clients
 * <p>
 * Description:
 */
@Service
public class AddressClientService {
    @Autowired
    private AddressFeignClient addressFeignClient;

    public AddressResponse getAddressById(Long addressId) {
        return addressFeignClient.getAddressById(addressId);
    }
}
