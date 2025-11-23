package com.commerce.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yixi Wan
 * @date 2025/11/19 14:42
 * @package com.commerce.order.dto
 * <p>
 * Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private Long addressId;
}

